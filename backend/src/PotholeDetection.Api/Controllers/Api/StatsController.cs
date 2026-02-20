using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using PotholeDetection.Api.DTOs;
using PotholeDetection.Api.Services;

namespace PotholeDetection.Api.Controllers.Api;

[ApiController]
[Route("api/stats")]
[Authorize]
public class StatsController : ControllerBase
{
    private readonly IStatsService _statsService;

    public StatsController(IStatsService statsService)
    {
        _statsService = statsService;
    }

    [HttpGet("overview")]
    public async Task<ActionResult<StatsOverview>> Overview()
    {
        var result = await _statsService.GetOverviewAsync();
        return Ok(result);
    }

    [HttpGet("daily")]
    public async Task<ActionResult<List<DailyStat>>> Daily([FromQuery] int days = 30)
    {
        var result = await _statsService.GetDailyAsync(days);
        return Ok(result);
    }

    [HttpGet("by-status")]
    public async Task<ActionResult<List<StatusStat>>> ByStatus()
    {
        var result = await _statsService.GetByStatusAsync();
        return Ok(result);
    }

    [HttpGet("by-vehicle")]
    public async Task<ActionResult<List<VehicleStat>>> ByVehicle()
    {
        var result = await _statsService.GetByVehicleAsync();
        return Ok(result);
    }

    [HttpGet("heatmap")]
    public async Task<ActionResult<List<HeatmapPoint>>> Heatmap()
    {
        var result = await _statsService.GetHeatmapAsync();
        return Ok(result);
    }
}
