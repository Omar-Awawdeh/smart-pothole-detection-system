namespace PotholeDetection.Api.Services;

public class LocalStorageService : IStorageService
{
    private readonly IWebHostEnvironment _env;
    private readonly string _baseUrl;

    public LocalStorageService(IWebHostEnvironment env, IConfiguration config)
    {
        _env = env;
        _baseUrl = config["Storage:PublicBaseUrl"] ?? "http://localhost:5073";
    }

    public async Task<string> UploadImageAsync(Stream imageStream, string vehicleId, string contentType = "image/jpeg")
    {
        var date = DateTime.UtcNow.ToString("yyyy-MM-dd");
        var filename = $"{Guid.NewGuid()}.jpg";
        var relativePath = Path.Combine("uploads", "potholes", vehicleId, date, filename);

        var fullPath = Path.Combine(_env.WebRootPath ?? Path.Combine(_env.ContentRootPath, "wwwroot"), relativePath);

        var dir = Path.GetDirectoryName(fullPath)!;
        Directory.CreateDirectory(dir);

        await using var fileStream = new FileStream(fullPath, FileMode.Create);
        await imageStream.CopyToAsync(fileStream);

        return $"{_baseUrl}/{relativePath.Replace('\\', '/')}";
    }
}
