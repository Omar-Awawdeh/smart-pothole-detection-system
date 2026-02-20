using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Options;
using Microsoft.IdentityModel.Tokens;
using PotholeDetection.Api.Configuration;
using PotholeDetection.Api.Data;
using PotholeDetection.Api.DTOs;
using PotholeDetection.Api.Models;

namespace PotholeDetection.Api.Services;

public interface IAuthService
{
    Task<AuthResponse> LoginAsync(LoginRequest request);
    Task<AuthResponse> RegisterAsync(RegisterRequest request);
    Task<AuthTokens> RefreshTokenAsync(string refreshToken);
    Task<UserDto> GetCurrentUserAsync(Guid userId);
}

public class AuthService : IAuthService
{
    private readonly AppDbContext _db;
    private readonly JwtSettings _jwt;

    public AuthService(AppDbContext db, IOptions<JwtSettings> jwt)
    {
        _db = db;
        _jwt = jwt.Value;
    }

    public async Task<AuthResponse> LoginAsync(LoginRequest request)
    {
        var user = await _db.Users.FirstOrDefaultAsync(u => u.Email == request.Email);

        if (user == null || !BCrypt.Net.BCrypt.Verify(request.Password, user.PasswordHash))
            throw new UnauthorizedAccessException("Invalid credentials");

        var tokens = GenerateTokens(user);

        return new AuthResponse
        {
            User = MapToDto(user),
            Tokens = tokens
        };
    }

    public async Task<AuthResponse> RegisterAsync(RegisterRequest request)
    {
        if (await _db.Users.AnyAsync(u => u.Email == request.Email))
            throw new InvalidOperationException("Email already exists");

        var user = new User
        {
            Email = request.Email,
            PasswordHash = BCrypt.Net.BCrypt.HashPassword(request.Password),
            Name = request.Name,
            Role = ParseRole(request.Role)
        };

        _db.Users.Add(user);
        await _db.SaveChangesAsync();

        var tokens = GenerateTokens(user);

        return new AuthResponse
        {
            User = MapToDto(user),
            Tokens = tokens
        };
    }

    public async Task<AuthTokens> RefreshTokenAsync(string refreshToken)
    {
        var principal = ValidateToken(refreshToken, _jwt.RefreshSecret);
        if (principal == null)
            throw new UnauthorizedAccessException("Invalid refresh token");

        var userIdClaim = principal.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        if (userIdClaim == null || !Guid.TryParse(userIdClaim, out var userId))
            throw new UnauthorizedAccessException("Invalid refresh token");

        var user = await _db.Users.FindAsync(userId);
        if (user == null)
            throw new UnauthorizedAccessException("User not found");

        return GenerateTokens(user);
    }

    public async Task<UserDto> GetCurrentUserAsync(Guid userId)
    {
        var user = await _db.Users.FindAsync(userId);
        if (user == null)
            throw new KeyNotFoundException("User not found");

        return MapToDto(user);
    }

    private AuthTokens GenerateTokens(User user)
    {
        var accessToken = GenerateJwtToken(user, _jwt.Secret, TimeSpan.FromMinutes(_jwt.AccessTokenExpirationMinutes));
        var refreshToken = GenerateJwtToken(user, _jwt.RefreshSecret, TimeSpan.FromDays(_jwt.RefreshTokenExpirationDays));

        return new AuthTokens
        {
            AccessToken = accessToken,
            RefreshToken = refreshToken
        };
    }

    private string GenerateJwtToken(User user, string secret, TimeSpan expiration)
    {
        var key = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(secret));
        var credentials = new SigningCredentials(key, SecurityAlgorithms.HmacSha256);

        var claims = new[]
        {
            new Claim(ClaimTypes.NameIdentifier, user.Id.ToString()),
            new Claim(ClaimTypes.Email, user.Email),
            new Claim(ClaimTypes.Role, user.Role.ToString().ToLower())
        };

        var token = new JwtSecurityToken(
            issuer: _jwt.Issuer,
            audience: _jwt.Audience,
            claims: claims,
            expires: DateTime.UtcNow.Add(expiration),
            signingCredentials: credentials
        );

        return new JwtSecurityTokenHandler().WriteToken(token);
    }

    private ClaimsPrincipal? ValidateToken(string token, string secret)
    {
        var tokenHandler = new JwtSecurityTokenHandler();
        var key = Encoding.UTF8.GetBytes(secret);

        try
        {
            return tokenHandler.ValidateToken(token, new TokenValidationParameters
            {
                ValidateIssuerSigningKey = true,
                IssuerSigningKey = new SymmetricSecurityKey(key),
                ValidateIssuer = true,
                ValidIssuer = _jwt.Issuer,
                ValidateAudience = true,
                ValidAudience = _jwt.Audience,
                ValidateLifetime = true,
                ClockSkew = TimeSpan.Zero
            }, out _);
        }
        catch
        {
            return null;
        }
    }

    private static UserDto MapToDto(User user)
    {
        return new UserDto
        {
            Id = user.Id,
            Email = user.Email,
            Name = user.Name,
            Role = user.Role.ToString().ToLower()
        };
    }

    private static UserRole ParseRole(string? role)
    {
        if (string.IsNullOrEmpty(role))
            return UserRole.Viewer;

        return Enum.TryParse<UserRole>(role, true, out var parsed) ? parsed : UserRole.Viewer;
    }
}
