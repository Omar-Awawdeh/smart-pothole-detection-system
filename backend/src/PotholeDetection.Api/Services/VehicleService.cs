using Microsoft.EntityFrameworkCore;
using PotholeDetection.Api.Data;
using PotholeDetection.Api.DTOs;
using PotholeDetection.Api.Models;

namespace PotholeDetection.Api.Services;

public interface IVehicleService
{
    Task<VehicleResponse> CreateAsync(VehicleCreateRequest request);
    Task<List<VehicleResponse>> ListAsync();
    Task<VehicleResponse?> GetByIdAsync(Guid id);
    Task<VehicleResponse?> UpdateAsync(Guid id, VehicleUpdateRequest request);
    Task<bool> DeleteAsync(Guid id);
    Task<PaginatedResponse<PotholeDetailResponse>> GetPotholesByVehicleAsync(Guid vehicleId, PotholeListQuery query);
}

public class VehicleService : IVehicleService
{
    private readonly AppDbContext _db;

    public VehicleService(AppDbContext db)
    {
        _db = db;
    }

    public async Task<VehicleResponse> CreateAsync(VehicleCreateRequest request)
    {
        if (await _db.Vehicles.AnyAsync(v => v.SerialNumber == request.SerialNumber))
            throw new InvalidOperationException("Serial number already exists");

        var vehicle = new Vehicle
        {
            Name = request.Name,
            SerialNumber = request.SerialNumber
        };

        _db.Vehicles.Add(vehicle);
        await _db.SaveChangesAsync();

        return MapToResponse(vehicle);
    }

    public async Task<List<VehicleResponse>> ListAsync()
    {
        var vehicles = await _db.Vehicles
            .Where(v => v.IsActive)
            .OrderByDescending(v => v.CreatedAt)
            .ToListAsync();

        return vehicles.Select(MapToResponse).ToList();
    }

    public async Task<VehicleResponse?> GetByIdAsync(Guid id)
    {
        var vehicle = await _db.Vehicles.FindAsync(id);
        return vehicle == null ? null : MapToResponse(vehicle);
    }

    public async Task<VehicleResponse?> UpdateAsync(Guid id, VehicleUpdateRequest request)
    {
        var vehicle = await _db.Vehicles.FindAsync(id);
        if (vehicle == null) return null;

        if (request.Name != null) vehicle.Name = request.Name;
        if (request.IsActive.HasValue) vehicle.IsActive = request.IsActive.Value;

        await _db.SaveChangesAsync();

        return MapToResponse(vehicle);
    }

    public async Task<bool> DeleteAsync(Guid id)
    {
        var vehicle = await _db.Vehicles.FindAsync(id);
        if (vehicle == null) return false;

        _db.Vehicles.Remove(vehicle);
        await _db.SaveChangesAsync();
        return true;
    }

    public async Task<PaginatedResponse<PotholeDetailResponse>> GetPotholesByVehicleAsync(Guid vehicleId, PotholeListQuery query)
    {
        var q = _db.Potholes.Where(p => p.VehicleId == vehicleId);

        var total = await q.CountAsync();
        var limit = Math.Clamp(query.Limit, 1, 100);
        var page = Math.Max(query.Page, 1);
        var offset = (page - 1) * limit;

        var potholes = await q
            .OrderByDescending(p => p.DetectedAt)
            .Skip(offset)
            .Take(limit)
            .ToListAsync();

        return new PaginatedResponse<PotholeDetailResponse>
        {
            Data = potholes.Select(p => new PotholeDetailResponse
            {
                Id = p.Id,
                Latitude = p.Latitude,
                Longitude = p.Longitude,
                Confidence = p.Confidence,
                ImageUrl = p.ImageUrl,
                Status = p.Status == PotholeStatus.FalsePositive ? "false_positive" : p.Status.ToString().ToLower(),
                Severity = p.Severity.ToString().ToLower(),
                ConfirmationCount = p.ConfirmationCount,
                DetectedAt = p.DetectedAt,
                RepairedAt = p.RepairedAt,
                CreatedAt = p.CreatedAt,
                UpdatedAt = p.UpdatedAt,
                VehicleId = p.VehicleId
            }).ToList(),
            Pagination = new PaginationMeta
            {
                Page = page,
                Limit = limit,
                Total = total,
                TotalPages = (int)Math.Ceiling((double)total / limit)
            }
        };
    }

    private static VehicleResponse MapToResponse(Vehicle v)
    {
        return new VehicleResponse
        {
            Id = v.Id,
            Name = v.Name,
            SerialNumber = v.SerialNumber,
            IsActive = v.IsActive,
            LastActiveAt = v.LastActiveAt,
            CreatedAt = v.CreatedAt
        };
    }
}
