using Microsoft.EntityFrameworkCore;
using PotholeDetection.Api.Models;

namespace PotholeDetection.Api.Data;

public class AppDbContext : DbContext
{
    public AppDbContext(DbContextOptions<AppDbContext> options) : base(options) { }

    public DbSet<User> Users => Set<User>();
    public DbSet<Vehicle> Vehicles => Set<Vehicle>();
    public DbSet<Pothole> Potholes => Set<Pothole>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        base.OnModelCreating(modelBuilder);

        modelBuilder.HasPostgresExtension("postgis");

        modelBuilder.Entity<User>(entity =>
        {
            entity.HasIndex(u => u.Email).IsUnique();

            entity.Property(u => u.Role)
                .HasConversion(
                    v => v.ToString().ToLower(),
                    v => Enum.Parse<UserRole>(v, true))
                .HasMaxLength(20);
        });

        modelBuilder.Entity<Vehicle>(entity =>
        {
            entity.HasIndex(v => v.SerialNumber).IsUnique();
        });

        modelBuilder.Entity<Pothole>(entity =>
        {
            entity.HasIndex(p => p.Status);
            entity.HasIndex(p => p.DetectedAt).IsDescending();
            entity.HasIndex(p => p.VehicleId);

            entity.Property(p => p.Location).HasColumnType("geography(Point, 4326)");

            entity.Property(p => p.Status)
                .HasConversion(
                    v => ConvertStatusToDb(v),
                    v => ConvertStatusFromDb(v))
                .HasMaxLength(20);

            entity.Property(p => p.Severity)
                .HasConversion(
                    v => v.ToString().ToLower(),
                    v => Enum.Parse<PotholeSeverity>(v, true))
                .HasMaxLength(10);

            entity.HasOne(p => p.Vehicle)
                .WithMany(v => v.Potholes)
                .HasForeignKey(p => p.VehicleId)
                .OnDelete(DeleteBehavior.Cascade);
        });
    }

    private static string ConvertStatusToDb(PotholeStatus status)
    {
        return status switch
        {
            PotholeStatus.FalsePositive => "false_positive",
            _ => status.ToString().ToLower()
        };
    }

    private static PotholeStatus ConvertStatusFromDb(string value)
    {
        return value switch
        {
            "false_positive" => PotholeStatus.FalsePositive,
            _ => Enum.Parse<PotholeStatus>(value, true)
        };
    }
}
