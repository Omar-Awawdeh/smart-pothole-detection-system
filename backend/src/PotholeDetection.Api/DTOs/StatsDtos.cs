namespace PotholeDetection.Api.DTOs;

public class StatsOverview
{
    public int Total { get; set; }
    public int Unverified { get; set; }
    public int Verified { get; set; }
    public int Repaired { get; set; }
    public int FalsePositive { get; set; }
    public int TodayCount { get; set; }
}

public class DailyStat
{
    public string Date { get; set; } = string.Empty;
    public int Count { get; set; }
}

public class StatusStat
{
    public string Status { get; set; } = string.Empty;
    public int Count { get; set; }
}

public class VehicleStat
{
    public Guid VehicleId { get; set; }
    public string VehicleName { get; set; } = string.Empty;
    public int Count { get; set; }
}

public class HeatmapPoint
{
    public double Latitude { get; set; }
    public double Longitude { get; set; }
    public int Intensity { get; set; }
}
