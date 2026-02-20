namespace PotholeDetection.Api.Configuration;

public class S3Settings
{
    public string AccessKeyId { get; set; } = string.Empty;
    public string SecretAccessKey { get; set; } = string.Empty;
    public string Region { get; set; } = "eu-central-1";
    public string BucketName { get; set; } = string.Empty;
}
