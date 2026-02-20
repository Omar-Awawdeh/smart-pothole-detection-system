using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using PotholeDetection.Api.DTOs;
using PotholeDetection.Api.Services;

namespace PotholeDetection.Api.Controllers.Api;

[ApiController]
[Route("api/vehicles")]
[Authorize]
public class VehicleController : ControllerBase
{
    private readonly IVehicleService _vehicleService;

    public VehicleController(IVehicleService vehicleService)
    {
        _vehicleService = vehicleService;
    }

    [Authorize(Roles = "admin")]
    [HttpPost]
    public async Task<ActionResult<VehicleResponse>> Create([FromBody] VehicleCreateRequest request)
    {
        var userId = Guid.Parse(User.FindFirst(ClaimTypes.NameIdentifier)!.Value);
        var result = await _vehicleService.CreateAsync(request, userId);
        return Created($"/api/vehicles/{result.Id}", result);
    }

    [HttpGet]
    public async Task<ActionResult<List<VehicleResponse>>> List()
    {
        var result = await _vehicleService.ListAsync();
        return Ok(result);
    }

    [HttpGet("{id:guid}")]
    public async Task<ActionResult<VehicleResponse>> Get(Guid id)
    {
        var result = await _vehicleService.GetByIdAsync(id);
        if (result == null) return NotFound(new { success = false, error = new { code = "NOT_FOUND", message = "Vehicle not found" } });
        return Ok(result);
    }

    [Authorize(Roles = "admin")]
    [HttpPatch("{id:guid}")]
    public async Task<ActionResult<VehicleResponse>> Update(Guid id, [FromBody] VehicleUpdateRequest request)
    {
        var result = await _vehicleService.UpdateAsync(id, request);
        if (result == null) return NotFound(new { success = false, error = new { code = "NOT_FOUND", message = "Vehicle not found" } });
        return Ok(result);
    }

    [Authorize(Roles = "admin")]
    [HttpDelete("{id:guid}")]
    public async Task<IActionResult> Delete(Guid id)
    {
        var deleted = await _vehicleService.DeleteAsync(id);
        if (!deleted) return NotFound(new { success = false, error = new { code = "NOT_FOUND", message = "Vehicle not found" } });
        return NoContent();
    }

    [HttpGet("{id:guid}/potholes")]
    public async Task<ActionResult<PaginatedResponse<PotholeDetailResponse>>> GetPotholes(Guid id, [FromQuery] PotholeListQuery query)
    {
        var result = await _vehicleService.GetPotholesByVehicleAsync(id, query);
        return Ok(result);
    }
}
