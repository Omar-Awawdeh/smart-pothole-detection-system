namespace PotholeDetection.Api.Configuration;

public class JwtSettings
{
    public string Secret { get; set; } = string.Empty;
    public string RefreshSecret { get; set; } = string.Empty;
    public int AccessTokenExpirationMinutes { get; set; } = 15;
    public int RefreshTokenExpirationDays { get; set; } = 7;
    public string Issuer { get; set; } = "PotholeDetection";
    public string Audience { get; set; } = "PotholeDetectionApp";
}
