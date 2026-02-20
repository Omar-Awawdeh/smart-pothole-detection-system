namespace PotholeDetection.Api.Services;

public class LocalStorageService : IStorageService
{
    private readonly IWebHostEnvironment _env;
    private readonly string _baseUrl;

    public LocalStorageService(IWebHostEnvironment env, IConfiguration config)
    {
        _env = env;

        var explicitBaseUrl = config["Storage:PublicBaseUrl"];
        if (!string.IsNullOrWhiteSpace(explicitBaseUrl))
        {
            _baseUrl = explicitBaseUrl;
            return;
        }

        var urls = config["ASPNETCORE_URLS"];
        if (!string.IsNullOrWhiteSpace(urls))
        {
            var firstUrl = urls
                .Split(';', StringSplitOptions.RemoveEmptyEntries)
                .Select(u => u.Replace("0.0.0.0", "localhost").Replace("+", "localhost"))
                .FirstOrDefault();

            if (!string.IsNullOrWhiteSpace(firstUrl))
            {
                _baseUrl = firstUrl;
                return;
            }
        }

        _baseUrl = "http://localhost:3000";
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
