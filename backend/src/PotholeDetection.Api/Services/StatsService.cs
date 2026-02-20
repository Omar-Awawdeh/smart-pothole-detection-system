using Microsoft.EntityFrameworkCore;
using PotholeDetection.Api.Data;
using PotholeDetection.Api.DTOs;
using PotholeDetection.Api.Models;

namespace PotholeDetection.Api.Services;

public interface IStatsService
{
    Task<StatsOverview> GetOverviewAsync();
    Task<List<DailyStat>> GetDailyAsync(int days);
    Task<List<StatusStat>> GetByStatusAsync();
    Task<List<VehicleStat>> GetByVehicleAsync();
    Task<List<HeatmapPoint>> GetHeatmapAsync();
}

public class StatsService : IStatsService
{
    private readonly AppDbContext _db;

    public StatsService(AppDbContext db)
    {
        _db = db;
    }

    public async Task<StatsOverview> GetOverviewAsync()
    {
        var today = DateTime.UtcNow.Date;

        return new StatsOverview
        {
            Total = await _db.Potholes.CountAsync(),
            Unverified = await _db.Potholes.CountAsync(p => p.Status == PotholeStatus.Unverified),
            Verified = await _db.Potholes.CountAsync(p => p.Status == PotholeStatus.Verified),
            Repaired = await _db.Potholes.CountAsync(p => p.Status == PotholeStatus.Repaired),
            FalsePositive = await _db.Potholes.CountAsync(p => p.Status == PotholeStatus.FalsePositive),
            TodayCount = await _db.Potholes.CountAsync(p => p.DetectedAt >= today)
        };
    }

    public async Task<List<DailyStat>> GetDailyAsync(int days)
    {
        var since = DateTime.UtcNow.Date.AddDays(-days);

        var raw = await _db.Potholes
            .Where(p => p.DetectedAt >= since)
            .GroupBy(p => p.DetectedAt.Date)
            .Select(g => new { Date = g.Key, Count = g.Count() })
            .OrderBy(g => g.Date)
            .ToListAsync();

        var stats = raw.Select(g => new DailyStat
        {
            Date = g.Date.ToString("yyyy-MM-dd"),
            Count = g.Count
        }).ToList();

        return stats;
    }

    public async Task<List<StatusStat>> GetByStatusAsync()
    {
        var stats = await _db.Potholes
            .GroupBy(p => p.Status)
            .Select(g => new { Status = g.Key, Count = g.Count() })
            .ToListAsync();

        return stats.Select(s => new StatusStat
        {
            Status = s.Status == PotholeStatus.FalsePositive ? "false_positive" : s.Status.ToString().ToLower(),
            Count = s.Count
        }).ToList();
    }

    public async Task<List<VehicleStat>> GetByVehicleAsync()
    {
        var stats = await _db.Potholes
            .Include(p => p.Vehicle)
            .GroupBy(p => new { p.VehicleId, p.Vehicle!.Name })
            .Select(g => new VehicleStat
            {
                VehicleId = g.Key.VehicleId,
                VehicleName = g.Key.Name,
                Count = g.Count()
            })
            .OrderByDescending(s => s.Count)
            .ToListAsync();

        return stats;
    }

    public async Task<List<HeatmapPoint>> GetHeatmapAsync()
    {
        var potholes = await _db.Potholes
            .Where(p => p.Status != PotholeStatus.FalsePositive)
            .Select(p => new HeatmapPoint
            {
                Latitude = p.Latitude,
                Longitude = p.Longitude,
                Intensity = p.ConfirmationCount
            })
            .ToListAsync();

        return potholes;
    }
}
