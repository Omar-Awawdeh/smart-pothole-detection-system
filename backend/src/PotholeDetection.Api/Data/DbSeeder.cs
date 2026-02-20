using Microsoft.EntityFrameworkCore;
using NetTopologySuite.Geometries;
using PotholeDetection.Api.Models;

namespace PotholeDetection.Api.Data;

public static class DbSeeder
{
    // Fixed IDs so the seeder is idempotent — running it twice adds nothing.
    private static readonly Guid AdminId    = new("11111111-0000-0000-0000-000000000001");
    private static readonly Guid OperatorId = new("11111111-0000-0000-0000-000000000002");
    private static readonly Guid ViewerId   = new("11111111-0000-0000-0000-000000000003");
    private static readonly Guid Vehicle1Id = new("22222222-0000-0000-0000-000000000001");
    private static readonly Guid Vehicle2Id = new("22222222-0000-0000-0000-000000000002");

    public static async Task SeedAsync(AppDbContext db)
    {
        // ── Users ──────────────────────────────────────────────────────────
        if (!await db.Users.AnyAsync(u => u.Id == AdminId))
        {
            db.Users.AddRange(
                new User
                {
                    Id           = AdminId,
                    Email        = "admin@pothole.dev",
                    PasswordHash = BCrypt.Net.BCrypt.HashPassword("admin123"),
                    Name         = "Admin User",
                    Role         = UserRole.Admin,
                    CreatedAt    = Ago(60),
                    UpdatedAt    = Ago(60)
                },
                new User
                {
                    Id           = OperatorId,
                    Email        = "operator@pothole.dev",
                    PasswordHash = BCrypt.Net.BCrypt.HashPassword("operator123"),
                    Name         = "Hamza Operator",
                    Role         = UserRole.Operator,
                    CreatedAt    = Ago(50),
                    UpdatedAt    = Ago(50)
                },
                new User
                {
                    Id           = ViewerId,
                    Email        = "viewer@pothole.dev",
                    PasswordHash = BCrypt.Net.BCrypt.HashPassword("viewer123"),
                    Name         = "Sara Viewer",
                    Role         = UserRole.Viewer,
                    CreatedAt    = Ago(45),
                    UpdatedAt    = Ago(45)
                }
            );
            await db.SaveChangesAsync();
        }

        // ── Vehicles ───────────────────────────────────────────────────────
        if (!await db.Vehicles.AnyAsync(v => v.Id == Vehicle1Id))
        {
            db.Vehicles.AddRange(
                new Vehicle
                {
                    Id           = Vehicle1Id,
                    UserId       = AdminId,
                    Name         = "Road Surveyor Alpha",
                    SerialNumber = "RSA-001-LHR",
                    IsActive     = true,
                    LastActiveAt = Ago(1),
                    CreatedAt    = Ago(55)
                },
                new Vehicle
                {
                    Id           = Vehicle2Id,
                    UserId       = OperatorId,
                    Name         = "Road Surveyor Beta",
                    SerialNumber = "RSB-002-LHR",
                    IsActive     = true,
                    LastActiveAt = Ago(2),
                    CreatedAt    = Ago(48)
                }
            );
            await db.SaveChangesAsync();
        }

        // ── Potholes ───────────────────────────────────────────────────────
        if (await db.Potholes.AnyAsync())
            return;

        // (lat, lon, daysAgo, status, severity, confidence, confirmations, imageIndex — null = no image)
        var entries = new (double Lat, double Lon, int DaysAgo, PotholeStatus Status,
                           PotholeSeverity Severity, double Conf, int Confirms, Guid VehicleId, int? ImgIdx)[]
        {
            // ── Gulberg / MM Alam Road ─────────────────────────────────────
            (31.5020, 74.3511,  1, PotholeStatus.Unverified,   PotholeSeverity.High,   0.97, 1, Vehicle1Id, 1),
            (31.5034, 74.3528,  2, PotholeStatus.Verified,     PotholeSeverity.Medium, 0.88, 3, Vehicle1Id, 2),
            (31.5009, 74.3494,  3, PotholeStatus.Unverified,   PotholeSeverity.Low,    0.72, 1, Vehicle2Id, null),
            (31.5041, 74.3549,  4, PotholeStatus.Repaired,     PotholeSeverity.High,   0.95, 5, Vehicle1Id, 3),
            (31.4998, 74.3480,  5, PotholeStatus.Verified,     PotholeSeverity.Medium, 0.84, 2, Vehicle2Id, 4),

            // ── DHA Phase 5 ────────────────────────────────────────────────
            (31.4712, 74.4033,  2, PotholeStatus.Unverified,   PotholeSeverity.High,   0.91, 1, Vehicle2Id, 5),
            (31.4698, 74.4019,  3, PotholeStatus.Verified,     PotholeSeverity.High,   0.93, 4, Vehicle1Id, 6),
            (31.4730, 74.4058,  6, PotholeStatus.Repaired,     PotholeSeverity.Low,    0.78, 6, Vehicle2Id, null),
            (31.4685, 74.3999,  8, PotholeStatus.FalsePositive,PotholeSeverity.Low,    0.51, 1, Vehicle1Id, 7),
            (31.4745, 74.4071, 10, PotholeStatus.Unverified,   PotholeSeverity.Medium, 0.82, 1, Vehicle2Id, 8),

            // ── Johar Town ─────────────────────────────────────────────────
            (31.4549, 74.2861,  1, PotholeStatus.Unverified,   PotholeSeverity.Medium, 0.86, 1, Vehicle1Id, 9),
            (31.4563, 74.2878,  4, PotholeStatus.Verified,     PotholeSeverity.High,   0.94, 3, Vehicle2Id, 10),
            (31.4535, 74.2847,  7, PotholeStatus.Repaired,     PotholeSeverity.High,   0.96, 7, Vehicle1Id, 11),
            (31.4578, 74.2893, 11, PotholeStatus.Unverified,   PotholeSeverity.Low,    0.67, 1, Vehicle2Id, null),
            (31.4521, 74.2830, 15, PotholeStatus.Verified,     PotholeSeverity.Medium, 0.80, 2, Vehicle1Id, 12),

            // ── Model Town ─────────────────────────────────────────────────
            (31.4697, 74.3321,  2, PotholeStatus.Unverified,   PotholeSeverity.High,   0.92, 1, Vehicle2Id, 13),
            (31.4711, 74.3337,  5, PotholeStatus.Verified,     PotholeSeverity.Medium, 0.85, 2, Vehicle1Id, 14),
            (31.4682, 74.3304,  9, PotholeStatus.Unverified,   PotholeSeverity.Low,    0.70, 1, Vehicle2Id, null),
            (31.4725, 74.3355, 13, PotholeStatus.Repaired,     PotholeSeverity.High,   0.98, 8, Vehicle1Id, 15),
            (31.4669, 74.3289, 18, PotholeStatus.FalsePositive,PotholeSeverity.Low,    0.48, 1, Vehicle2Id, 16),

            // ── Shadman / Garden Town ──────────────────────────────────────
            (31.5290, 74.3334,  1, PotholeStatus.Unverified,   PotholeSeverity.High,   0.95, 1, Vehicle1Id, 17),
            (31.4918, 74.3277,  3, PotholeStatus.Verified,     PotholeSeverity.Medium, 0.87, 3, Vehicle2Id, 18),
            (31.5278, 74.3319,  6, PotholeStatus.Unverified,   PotholeSeverity.Low,    0.74, 1, Vehicle1Id, null),
            (31.4930, 74.3292, 10, PotholeStatus.Verified,     PotholeSeverity.High,   0.90, 4, Vehicle2Id, 19),
            (31.5302, 74.3350, 14, PotholeStatus.Repaired,     PotholeSeverity.Medium, 0.83, 5, Vehicle1Id, 20),

            // ── Cavalry Ground / Cantt ─────────────────────────────────────
            (31.5234, 74.3807,  2, PotholeStatus.Unverified,   PotholeSeverity.Medium, 0.88, 1, Vehicle2Id, 21),
            (31.5219, 74.3792,  5, PotholeStatus.Verified,     PotholeSeverity.High,   0.93, 3, Vehicle1Id, 22),
            (31.5248, 74.3823,  8, PotholeStatus.Unverified,   PotholeSeverity.Low,    0.65, 1, Vehicle2Id, null),
            (31.5261, 74.3838, 20, PotholeStatus.Repaired,     PotholeSeverity.High,   0.97, 9, Vehicle1Id, 23),
            (31.5205, 74.3776, 25, PotholeStatus.FalsePositive,PotholeSeverity.Low,    0.44, 1, Vehicle2Id, 24),

            // ── Iqbal Town / Wapda Town ────────────────────────────────────
            (31.4781, 74.3047,  3, PotholeStatus.Unverified,   PotholeSeverity.High,   0.89, 1, Vehicle1Id, 25),
            (31.4389, 74.2680,  7, PotholeStatus.Verified,     PotholeSeverity.Medium, 0.81, 2, Vehicle2Id, 26),
        };

        var geomFactory = NetTopologySuite.NtsGeometryServices.Instance.CreateGeometryFactory(srid: 4326);

        var potholes = entries.Select(e =>
        {
            var detectedAt = Ago(e.DaysAgo);
            // Stable placeholder images — picsum seeds give the same image every time
            var imageUrl = e.ImgIdx.HasValue
                ? $"https://picsum.photos/seed/pothole{e.ImgIdx}/800/600"
                : null;
            return new Pothole
            {
                Id                = Guid.NewGuid(),
                VehicleId         = e.VehicleId,
                Latitude          = e.Lat,
                Longitude         = e.Lon,
                Location          = geomFactory.CreatePoint(new Coordinate(e.Lon, e.Lat)),
                Confidence        = e.Conf,
                ImageUrl          = imageUrl,
                Status            = e.Status,
                Severity          = e.Severity,
                ConfirmationCount = e.Confirms,
                DetectedAt        = detectedAt,
                RepairedAt        = e.Status == PotholeStatus.Repaired ? detectedAt.AddDays(2) : null,
                CreatedAt         = detectedAt,
                UpdatedAt         = detectedAt
            };
        }).ToList();

        db.Potholes.AddRange(potholes);
        await db.SaveChangesAsync();
    }

    private static DateTime Ago(int days) =>
        DateTime.UtcNow.AddDays(-days);
}
