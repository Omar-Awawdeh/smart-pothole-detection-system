using System.Text.Json.Serialization;

namespace PotholeDetection.Api.DTOs;

public class VehicleCreateRequest
{
    public string Name { get; set; } = string.Empty;
    public string SerialNumber { get; set; } = string.Empty;
}

public class VehicleUpdateRequest
{
    public string? Name { get; set; }
    public bool? IsActive { get; set; }
}

public class VehicleResponse
{
    public Guid Id { get; set; }

    [JsonPropertyName("user_id")]
    public Guid UserId { get; set; }

    public string Name { get; set; } = string.Empty;

    [JsonPropertyName("serial_number")]
    public string SerialNumber { get; set; } = string.Empty;

    [JsonPropertyName("is_active")]
    public bool IsActive { get; set; }

    [JsonPropertyName("last_active_at")]
    public DateTime? LastActiveAt { get; set; }

    [JsonPropertyName("created_at")]
    public DateTime CreatedAt { get; set; }
}
