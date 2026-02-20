using Microsoft.EntityFrameworkCore;
using NetTopologySuite.Geometries;
using PotholeDetection.Api.Data;
using PotholeDetection.Api.DTOs;
using PotholeDetection.Api.Models;

namespace PotholeDetection.Api.Services;

public interface IPotholeService
{
    Task<PotholeResponse> CreateAsync(double latitude, double longitude, double confidence,
        string vehicleId, long timestamp, Stream? imageStream);
    Task<PaginatedResponse<PotholeDetailResponse>> ListAsync(PotholeListQuery query);
    Task<PotholeDetailResponse?> GetByIdAsync(Guid id);
    Task<PotholeDetailResponse?> UpdateAsync(Guid id, PotholeUpdateRequest request);
    Task<bool> DeleteAsync(Guid id);
}

public class PotholeService : IPotholeService
{
    private readonly AppDbContext _db;
    private readonly IStorageService _storage;
    private const double DeduplicationRadiusMeters = 15;

    public PotholeService(AppDbContext db, IStorageService storage)
    {
        _db = db;
        _storage = storage;
    }

    public async Task<PotholeResponse> CreateAsync(double latitude, double longitude, double confidence,
        string vehicleId, long timestamp, Stream? imageStream)
    {
        var point = new Point(longitude, latitude) { SRID = 4326 };
        var detectedAt = DateTimeOffset.FromUnixTimeMilliseconds(timestamp).UtcDateTime;

        if (!Guid.TryParse(vehicleId, out var vehicleGuid))
            throw new ArgumentException("Invalid vehicleId format");

        var existing = await _db.Potholes
            .Where(p => p.Status != PotholeStatus.Repaired && p.Status != PotholeStatus.FalsePositive)
            .Where(p => p.Location.Distance(point) <= DeduplicationRadiusMeters)
            .OrderBy(p => p.Location.Distance(point))
            .FirstOrDefaultAsync();

        if (existing != null)
        {
            existing.ConfirmationCount++;
            existing.UpdatedAt = DateTime.UtcNow;
            await _db.SaveChangesAsync();

            return new PotholeResponse
            {
                Id = existing.Id,
                IsDuplicate = true,
                ExistingId = existing.Id,
                ConfirmationCount = existing.ConfirmationCount,
                Latitude = existing.Latitude,
                Longitude = existing.Longitude,
                Confidence = existing.Confidence,
                ImageUrl = existing.ImageUrl,
                Status = ConvertStatus(existing.Status),
                Severity = existing.Severity.ToString().ToLower(),
                DetectedAt = existing.DetectedAt
            };
        }

        string? imageUrl = null;
        if (imageStream != null)
        {
            imageUrl = await _storage.UploadImageAsync(imageStream, vehicleId);
        }

        var severity = CalculateSeverity(confidence);

        var pothole = new Pothole
        {
            VehicleId = vehicleGuid,
            Latitude = latitude,
            Longitude = longitude,
            Location = point,
            Confidence = confidence,
            ImageUrl = imageUrl,
            Status = PotholeStatus.Unverified,
            Severity = severity,
            DetectedAt = detectedAt
        };

        _db.Potholes.Add(pothole);

        var vehicle = await _db.Vehicles.FindAsync(vehicleGuid);
        if (vehicle != null)
        {
            vehicle.LastActiveAt = DateTime.UtcNow;
        }

        await _db.SaveChangesAsync();

        return new PotholeResponse
        {
            Id = pothole.Id,
            IsDuplicate = false,
            ExistingId = null,
            ConfirmationCount = null,
            Latitude = pothole.Latitude,
            Longitude = pothole.Longitude,
            Confidence = pothole.Confidence,
            ImageUrl = pothole.ImageUrl,
            Status = ConvertStatus(pothole.Status),
            Severity = pothole.Severity.ToString().ToLower(),
            DetectedAt = pothole.DetectedAt
        };
    }

    public async Task<PaginatedResponse<PotholeDetailResponse>> ListAsync(PotholeListQuery query)
    {
        var q = _db.Potholes.AsQueryable();

        if (!string.IsNullOrEmpty(query.Status))
        {
            var status = ParseStatus(query.Status);
            if (status.HasValue)
                q = q.Where(p => p.Status == status.Value);
        }

        if (!string.IsNullOrEmpty(query.Severity))
        {
            if (Enum.TryParse<PotholeSeverity>(query.Severity, true, out var severity))
                q = q.Where(p => p.Severity == severity);
        }

        if (query.VehicleId.HasValue)
            q = q.Where(p => p.VehicleId == query.VehicleId.Value);

        if (query.StartDate.HasValue)
            q = q.Where(p => p.DetectedAt >= query.StartDate.Value);

        if (query.EndDate.HasValue)
            q = q.Where(p => p.DetectedAt <= query.EndDate.Value);

        var total = await q.CountAsync();

        q = query.SortBy?.ToLower() switch
        {
            "confidence" => query.SortOrder?.ToUpper() == "ASC"
                ? q.OrderBy(p => p.Confidence)
                : q.OrderByDescending(p => p.Confidence),
            "status" => query.SortOrder?.ToUpper() == "ASC"
                ? q.OrderBy(p => p.Status)
                : q.OrderByDescending(p => p.Status),
            _ => query.SortOrder?.ToUpper() == "ASC"
                ? q.OrderBy(p => p.DetectedAt)
                : q.OrderByDescending(p => p.DetectedAt)
        };

        var limit = Math.Clamp(query.Limit, 1, 100);
        var page = Math.Max(query.Page, 1);
        var offset = (page - 1) * limit;

        var potholes = await q.Skip(offset).Take(limit).ToListAsync();

        return new PaginatedResponse<PotholeDetailResponse>
        {
            Data = potholes.Select(MapToDetail).ToList(),
            Pagination = new PaginationMeta
            {
                Page = page,
                Limit = limit,
                Total = total,
                TotalPages = (int)Math.Ceiling((double)total / limit)
            }
        };
    }

    public async Task<PotholeDetailResponse?> GetByIdAsync(Guid id)
    {
        var pothole = await _db.Potholes.FindAsync(id);
        return pothole == null ? null : MapToDetail(pothole);
    }

    public async Task<PotholeDetailResponse?> UpdateAsync(Guid id, PotholeUpdateRequest request)
    {
        var pothole = await _db.Potholes.FindAsync(id);
        if (pothole == null) return null;

        if (!string.IsNullOrEmpty(request.Status))
        {
            var status = ParseStatus(request.Status);
            if (status.HasValue)
            {
                pothole.Status = status.Value;
                if (status.Value == PotholeStatus.Repaired)
                    pothole.RepairedAt = DateTime.UtcNow;
            }
        }

        if (!string.IsNullOrEmpty(request.Severity))
        {
            if (Enum.TryParse<PotholeSeverity>(request.Severity, true, out var severity))
                pothole.Severity = severity;
        }

        pothole.UpdatedAt = DateTime.UtcNow;
        await _db.SaveChangesAsync();

        return MapToDetail(pothole);
    }

    public async Task<bool> DeleteAsync(Guid id)
    {
        var pothole = await _db.Potholes.FindAsync(id);
        if (pothole == null) return false;

        _db.Potholes.Remove(pothole);
        await _db.SaveChangesAsync();
        return true;
    }

    private static PotholeSeverity CalculateSeverity(double confidence)
    {
        return confidence switch
        {
            >= 0.8 => PotholeSeverity.High,
            >= 0.6 => PotholeSeverity.Medium,
            _ => PotholeSeverity.Low
        };
    }

    private static PotholeDetailResponse MapToDetail(Pothole p)
    {
        return new PotholeDetailResponse
        {
            Id = p.Id,
            Latitude = p.Latitude,
            Longitude = p.Longitude,
            Confidence = p.Confidence,
            ImageUrl = p.ImageUrl,
            Status = ConvertStatus(p.Status),
            Severity = p.Severity.ToString().ToLower(),
            ConfirmationCount = p.ConfirmationCount,
            DetectedAt = p.DetectedAt,
            RepairedAt = p.RepairedAt,
            CreatedAt = p.CreatedAt,
            UpdatedAt = p.UpdatedAt,
            VehicleId = p.VehicleId
        };
    }

    private static string ConvertStatus(PotholeStatus status)
    {
        return status switch
        {
            PotholeStatus.FalsePositive => "false_positive",
            _ => status.ToString().ToLower()
        };
    }

    private static PotholeStatus? ParseStatus(string value)
    {
        return value.ToLower() switch
        {
            "unverified" => PotholeStatus.Unverified,
            "verified" => PotholeStatus.Verified,
            "repaired" => PotholeStatus.Repaired,
            "false_positive" => PotholeStatus.FalsePositive,
            _ => null
        };
    }
}
