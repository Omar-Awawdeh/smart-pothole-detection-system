using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using NetTopologySuite.Geometries;

namespace PotholeDetection.Api.Models;

public enum PotholeStatus
{
    Unverified,
    Verified,
    Repaired,
    FalsePositive
}

public enum PotholeSeverity
{
    Low,
    Medium,
    High
}

[Table("potholes")]
public class Pothole
{
    [Key]
    [Column("id")]
    public Guid Id { get; set; } = Guid.NewGuid();

    [Required]
    [Column("vehicle_id")]
    public Guid VehicleId { get; set; }

    [Required]
    [Column("latitude")]
    public double Latitude { get; set; }

    [Required]
    [Column("longitude")]
    public double Longitude { get; set; }

    [Required]
    [Column("location", TypeName = "geography(Point, 4326)")]
    public Point Location { get; set; } = null!;

    [Required]
    [Column("confidence")]
    public double Confidence { get; set; }

    [MaxLength(500)]
    [Column("image_url")]
    public string? ImageUrl { get; set; }

    [Required]
    [Column("status")]
    public PotholeStatus Status { get; set; } = PotholeStatus.Unverified;

    [Required]
    [Column("severity")]
    public PotholeSeverity Severity { get; set; } = PotholeSeverity.Medium;

    [Column("confirmation_count")]
    public int ConfirmationCount { get; set; } = 1;

    [Required]
    [Column("detected_at")]
    public DateTime DetectedAt { get; set; }

    [Column("repaired_at")]
    public DateTime? RepairedAt { get; set; }

    [Column("created_at")]
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    [Column("updated_at")]
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;

    [ForeignKey("VehicleId")]
    public Vehicle? Vehicle { get; set; }
}
