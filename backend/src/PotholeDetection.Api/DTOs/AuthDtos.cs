using System.Text.Json.Serialization;

namespace PotholeDetection.Api.DTOs;

public class LoginRequest
{
    public string Email { get; set; } = string.Empty;
    public string Password { get; set; } = string.Empty;
}

public class RegisterRequest
{
    public string Email { get; set; } = string.Empty;
    public string Password { get; set; } = string.Empty;
    public string Name { get; set; } = string.Empty;
    public string? Role { get; set; }
}

public class RefreshRequest
{
    [JsonPropertyName("refresh_token")]
    public string RefreshToken { get; set; } = string.Empty;
}

public class AuthResponse
{
    public UserDto User { get; set; } = null!;
    public AuthTokens Tokens { get; set; } = null!;
}

public class AuthTokens
{
    [JsonPropertyName("access_token")]
    public string AccessToken { get; set; } = string.Empty;

    [JsonPropertyName("refresh_token")]
    public string RefreshToken { get; set; } = string.Empty;
}

public class UserDto
{
    public Guid Id { get; set; }
    public string Email { get; set; } = string.Empty;
    public string Name { get; set; } = string.Empty;
    public string Role { get; set; } = string.Empty;
}

public class UpdateProfileRequest
{
    public string? Name { get; set; }
    public string? Email { get; set; }
    public string? CurrentPassword { get; set; }
    public string? NewPassword { get; set; }
}
