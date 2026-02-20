using System.Text.Json.Serialization;

namespace PotholeDetection.Api.DTOs;

public class PotholeResponse
{
    public Guid Id { get; set; }
    public bool IsDuplicate { get; set; }
    public Guid? ExistingId { get; set; }
    public int? ConfirmationCount { get; set; }
    public double? Latitude { get; set; }
    public double? Longitude { get; set; }
    public double? Confidence { get; set; }

    [JsonPropertyName("image_url")]
    public string? ImageUrl { get; set; }

    public string? Status { get; set; }
    public string? Severity { get; set; }

    [JsonPropertyName("detected_at")]
    public DateTime? DetectedAt { get; set; }
}

public class PotholeDetailResponse
{
    public Guid Id { get; set; }
    public double Latitude { get; set; }
    public double Longitude { get; set; }
    public double Confidence { get; set; }

    [JsonPropertyName("image_url")]
    public string? ImageUrl { get; set; }

    public string Status { get; set; } = string.Empty;
    public string Severity { get; set; } = string.Empty;
    public int ConfirmationCount { get; set; }

    [JsonPropertyName("detected_at")]
    public DateTime DetectedAt { get; set; }

    [JsonPropertyName("repaired_at")]
    public DateTime? RepairedAt { get; set; }

    [JsonPropertyName("created_at")]
    public DateTime CreatedAt { get; set; }

    [JsonPropertyName("updated_at")]
    public DateTime UpdatedAt { get; set; }

    [JsonPropertyName("vehicle_id")]
    public Guid VehicleId { get; set; }
}

public class PotholeUpdateRequest
{
    public string? Status { get; set; }
    public string? Severity { get; set; }
}

public class PotholeListQuery
{
    public int Page { get; set; } = 1;
    public int Limit { get; set; } = 20;
    public string? Status { get; set; }
    public string? Severity { get; set; }
    public Guid? VehicleId { get; set; }
    public DateTime? StartDate { get; set; }
    public DateTime? EndDate { get; set; }
    public string? SortBy { get; set; } = "detected_at";
    public string? SortOrder { get; set; } = "DESC";
}

public class PaginatedResponse<T>
{
    public List<T> Data { get; set; } = new();
    public PaginationMeta Pagination { get; set; } = null!;
}

public class PaginationMeta
{
    public int Page { get; set; }
    public int Limit { get; set; }
    public int Total { get; set; }
    public int TotalPages { get; set; }
}

public class BulkActionRequest
{
    public List<Guid> Ids { get; set; } = new();
    public string Action { get; set; } = string.Empty; // "verify" | "repair" | "false_positive" | "delete"
}

public class BulkActionResponse
{
    public int Affected { get; set; }
}
