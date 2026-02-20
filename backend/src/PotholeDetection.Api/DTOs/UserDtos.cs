namespace PotholeDetection.Api.DTOs;

public class UserCreateRequest
{
    public string Email { get; set; } = string.Empty;
    public string Password { get; set; } = string.Empty;
    public string Name { get; set; } = string.Empty;
    public string? Role { get; set; }
}

public class UserUpdateRequest
{
    public string? Name { get; set; }
    public string? Role { get; set; }
    public string? Email { get; set; }
}
