using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using PotholeDetection.Api.DTOs;
using PotholeDetection.Api.Services;

namespace PotholeDetection.Api.Controllers.Api;

[ApiController]
[Route("api/users")]
[Authorize(Roles = "admin")]
public class UserController : ControllerBase
{
    private readonly IUserService _userService;

    public UserController(IUserService userService)
    {
        _userService = userService;
    }

    [HttpGet]
    public async Task<ActionResult<List<UserDto>>> List()
    {
        var result = await _userService.ListAsync();
        return Ok(result);
    }

    [HttpPost]
    public async Task<ActionResult<UserDto>> Create([FromBody] UserCreateRequest request)
    {
        var user = await _userService.CreateAsync(request);
        return Created(string.Empty, user);
    }

    [HttpGet("{id:guid}")]
    public async Task<ActionResult<UserDto>> Get(Guid id)
    {
        var result = await _userService.GetByIdAsync(id);
        if (result == null) return NotFound(new { success = false, error = new { code = "NOT_FOUND", message = "User not found" } });
        return Ok(result);
    }

    [HttpPatch("{id:guid}")]
    public async Task<ActionResult<UserDto>> Update(Guid id, [FromBody] UserUpdateRequest request)
    {
        var result = await _userService.UpdateAsync(id, request);
        if (result == null) return NotFound(new { success = false, error = new { code = "NOT_FOUND", message = "User not found" } });
        return Ok(result);
    }

    [HttpDelete("{id:guid}")]
    public async Task<IActionResult> Delete(Guid id)
    {
        var deleted = await _userService.DeleteAsync(id);
        if (!deleted) return NotFound(new { success = false, error = new { code = "NOT_FOUND", message = "User not found" } });
        return NoContent();
    }
}
