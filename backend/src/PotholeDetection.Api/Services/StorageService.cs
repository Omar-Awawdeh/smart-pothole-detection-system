using Amazon.S3;
using Amazon.S3.Model;
using Microsoft.Extensions.Options;
using PotholeDetection.Api.Configuration;

namespace PotholeDetection.Api.Services;

public interface IStorageService
{
    Task<string> UploadImageAsync(Stream imageStream, string vehicleId, string contentType = "image/jpeg");
}

public class S3StorageService : IStorageService
{
    private readonly IAmazonS3 _s3;
    private readonly S3Settings _settings;

    public S3StorageService(IAmazonS3 s3, IOptions<S3Settings> settings)
    {
        _s3 = s3;
        _settings = settings.Value;
    }

    public async Task<string> UploadImageAsync(Stream imageStream, string vehicleId, string contentType = "image/jpeg")
    {
        var date = DateTime.UtcNow.ToString("yyyy-MM-dd");
        var filename = $"{Guid.NewGuid()}.jpg";
        var key = $"potholes/{vehicleId}/{date}/{filename}";

        var request = new PutObjectRequest
        {
            BucketName = _settings.BucketName,
            Key = key,
            InputStream = imageStream,
            ContentType = contentType
        };

        await _s3.PutObjectAsync(request);

        if (!string.IsNullOrWhiteSpace(_settings.PublicBaseUrl))
        {
            return $"{_settings.PublicBaseUrl.TrimEnd('/')}/{key}";
        }

        if (!string.IsNullOrWhiteSpace(_settings.Endpoint))
        {
            return $"{_settings.Endpoint.TrimEnd('/')}/{_settings.BucketName}/{key}";
        }

        return $"https://{_settings.BucketName}.s3.{_settings.Region}.amazonaws.com/{key}";
    }
}
