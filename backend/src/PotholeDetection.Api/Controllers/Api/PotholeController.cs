using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using PotholeDetection.Api.DTOs;
using PotholeDetection.Api.Services;

namespace PotholeDetection.Api.Controllers.Api;

[ApiController]
[Route("api/potholes")]
[Authorize]
public class PotholeController : ControllerBase
{
    private readonly IPotholeService _potholeService;

    public PotholeController(IPotholeService potholeService)
    {
        _potholeService = potholeService;
    }

    [HttpPost]
    public async Task<ActionResult<PotholeResponse>> Create()
    {
        var form = await Request.ReadFormAsync();

        if (!double.TryParse(form["latitude"], out var latitude))
            return BadRequest(new { success = false, error = new { code = "VALIDATION_ERROR", message = "Invalid latitude" } });
        if (!double.TryParse(form["longitude"], out var longitude))
            return BadRequest(new { success = false, error = new { code = "VALIDATION_ERROR", message = "Invalid longitude" } });
        if (!double.TryParse(form["confidence"], out var confidence))
            return BadRequest(new { success = false, error = new { code = "VALIDATION_ERROR", message = "Invalid confidence" } });
        if (!long.TryParse(form["timestamp"], out var timestamp))
            return BadRequest(new { success = false, error = new { code = "VALIDATION_ERROR", message = "Invalid timestamp" } });

        var vehicleId = form["vehicleId"].ToString();
        if (string.IsNullOrEmpty(vehicleId))
            return BadRequest(new { success = false, error = new { code = "VALIDATION_ERROR", message = "vehicleId is required" } });

        Stream? imageStream = null;
        var imageFile = form.Files.GetFile("image");
        if (imageFile != null)
        {
            imageStream = imageFile.OpenReadStream();
        }

        var result = await _potholeService.CreateAsync(latitude, longitude, confidence, vehicleId, timestamp, imageStream);
        return Ok(result);
    }

    [HttpGet]
    public async Task<ActionResult<PaginatedResponse<PotholeDetailResponse>>> List([FromQuery] PotholeListQuery query)
    {
        var result = await _potholeService.ListAsync(query);
        return Ok(result);
    }

    [HttpGet("{id:guid}")]
    public async Task<ActionResult<PotholeDetailResponse>> Get(Guid id)
    {
        var result = await _potholeService.GetByIdAsync(id);
        if (result == null) return NotFound(new { success = false, error = new { code = "NOT_FOUND", message = "Pothole not found" } });
        return Ok(result);
    }

    [Authorize(Roles = "admin,operator")]
    [HttpPatch("{id:guid}")]
    public async Task<ActionResult<PotholeDetailResponse>> Update(Guid id, [FromBody] PotholeUpdateRequest request)
    {
        var result = await _potholeService.UpdateAsync(id, request);
        if (result == null) return NotFound(new { success = false, error = new { code = "NOT_FOUND", message = "Pothole not found" } });
        return Ok(result);
    }

    [Authorize(Roles = "admin")]
    [HttpDelete("{id:guid}")]
    public async Task<IActionResult> Delete(Guid id)
    {
        var deleted = await _potholeService.DeleteAsync(id);
        if (!deleted) return NotFound(new { success = false, error = new { code = "NOT_FOUND", message = "Pothole not found" } });
        return NoContent();
    }
}
