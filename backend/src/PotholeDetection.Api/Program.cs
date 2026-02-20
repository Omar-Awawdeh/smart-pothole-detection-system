using System.Text;
using Amazon.S3;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;
using Microsoft.OpenApi.Models;
using PotholeDetection.Api.Configuration;
using PotholeDetection.Api.Data;
using PotholeDetection.Api.Middleware;
using PotholeDetection.Api.Hubs;
using PotholeDetection.Api.Services;

var builder = WebApplication.CreateBuilder(args);

// ---------------------------------------------------------------------------
// Configuration bindings
// ---------------------------------------------------------------------------
builder.Services.Configure<JwtSettings>(builder.Configuration.GetSection("Jwt"));
builder.Services.Configure<S3Settings>(builder.Configuration.GetSection("S3"));

var jwtSettings = builder.Configuration.GetSection("Jwt").Get<JwtSettings>()
    ?? throw new InvalidOperationException("JWT settings are not configured");

// ---------------------------------------------------------------------------
// Database – PostgreSQL + PostGIS (NetTopologySuite)
// ---------------------------------------------------------------------------
builder.Services.AddDbContext<AppDbContext>(options =>
    options.UseNpgsql(
        builder.Configuration.GetConnectionString("DefaultConnection"),
        npgsql => npgsql.UseNetTopologySuite()));

// ---------------------------------------------------------------------------
// Authentication – JWT Bearer
// ---------------------------------------------------------------------------
builder.Services.AddAuthentication(options =>
{
    options.DefaultAuthenticateScheme = JwtBearerDefaults.AuthenticationScheme;
    options.DefaultChallengeScheme = JwtBearerDefaults.AuthenticationScheme;
})
.AddJwtBearer(options =>
{
    options.TokenValidationParameters = new TokenValidationParameters
    {
        ValidateIssuerSigningKey = true,
        IssuerSigningKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(jwtSettings.Secret)),
        ValidateIssuer = true,
        ValidIssuer = jwtSettings.Issuer,
        ValidateAudience = true,
        ValidAudience = jwtSettings.Audience,
        ValidateLifetime = true,
        ClockSkew = TimeSpan.Zero
    };
});

builder.Services.AddAuthorization();

// ---------------------------------------------------------------------------
// CORS – allow the React dashboard (dev & prod origins)
// ---------------------------------------------------------------------------
builder.Services.AddCors(options =>
{
    options.AddPolicy("Dashboard", policy =>
    {
        policy.WithOrigins(
                builder.Configuration.GetSection("Cors:Origins").Get<string[]>()
                    ?? new[] { "http://localhost:5173", "http://localhost:3000" })
            .AllowAnyHeader()
            .AllowAnyMethod()
            .AllowCredentials();
    });
});

// ---------------------------------------------------------------------------
// AWS S3 client
// ---------------------------------------------------------------------------
var s3Settings = builder.Configuration.GetSection("S3").Get<S3Settings>();
if (s3Settings is not null && !string.IsNullOrEmpty(s3Settings.AccessKeyId))
{
    builder.Services.AddSingleton<IAmazonS3>(_ =>
    {
        var config = new AmazonS3Config
        {
            RegionEndpoint = Amazon.RegionEndpoint.GetBySystemName(s3Settings.Region)
        };
        return new AmazonS3Client(s3Settings.AccessKeyId, s3Settings.SecretAccessKey, config);
    });
}
else
{
    builder.Services.AddSingleton<IAmazonS3>(_ =>
    {
        var region = s3Settings?.Region ?? "eu-central-1";
        var config = new AmazonS3Config
        {
            RegionEndpoint = Amazon.RegionEndpoint.GetBySystemName(region)
        };
        return new AmazonS3Client(config);
    });
}

// ---------------------------------------------------------------------------
// Application services (DI)
// ---------------------------------------------------------------------------
builder.Services.AddScoped<IAuthService, AuthService>();
builder.Services.AddScoped<IPotholeService, PotholeService>();
builder.Services.AddScoped<IVehicleService, VehicleService>();
builder.Services.AddScoped<IUserService, UserService>();
builder.Services.AddScoped<IStatsService, StatsService>();
builder.Services.AddScoped<IStorageService, S3StorageService>();

// ---------------------------------------------------------------------------
// Response compression (gzip for JSON/text — not images, they're already compressed)
// ---------------------------------------------------------------------------
builder.Services.AddResponseCompression(options =>
{
    options.EnableForHttps = true;
});

// ---------------------------------------------------------------------------
// Controllers + JSON
// ---------------------------------------------------------------------------
builder.Services.AddControllers();

builder.Services.AddSignalR();

// ---------------------------------------------------------------------------
// Swagger / OpenAPI
// ---------------------------------------------------------------------------
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen(options =>
{
    options.SwaggerDoc("v1", new OpenApiInfo
    {
        Title = "Pothole Detection API",
        Version = "v1",
        Description = "Backend API for the Pothole Detection graduation project"
    });

    options.AddSecurityDefinition("Bearer", new OpenApiSecurityScheme
    {
        Name = "Authorization",
        Type = SecuritySchemeType.Http,
        Scheme = "bearer",
        BearerFormat = "JWT",
        In = ParameterLocation.Header,
        Description = "Enter your JWT token"
    });

    options.AddSecurityRequirement(new OpenApiSecurityRequirement
    {
        {
            new OpenApiSecurityScheme
            {
                Reference = new OpenApiReference
                {
                    Type = ReferenceType.SecurityScheme,
                    Id = "Bearer"
                }
            },
            Array.Empty<string>()
        }
    });
});

// ---------------------------------------------------------------------------
// Health checks
// ---------------------------------------------------------------------------
builder.Services.AddHealthChecks();

// ---------------------------------------------------------------------------
// Build app
// ---------------------------------------------------------------------------
var app = builder.Build();

// ---------------------------------------------------------------------------
// Seed development data
// ---------------------------------------------------------------------------
using (var scope = app.Services.CreateScope())
{
    var db = scope.ServiceProvider.GetRequiredService<AppDbContext>();
    await DbSeeder.SeedAsync(db);
}

// ---------------------------------------------------------------------------
// Middleware pipeline
// ---------------------------------------------------------------------------
app.UseResponseCompression();
app.UseMiddleware<ErrorHandlingMiddleware>();

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI(options =>
    {
        options.SwaggerEndpoint("/swagger/v1/swagger.json", "Pothole Detection API v1");
    });
}

app.UseCors("Dashboard");

app.UseAuthentication();
app.UseAuthorization();

app.MapControllers();
app.MapHub<PotholeHub>("/hubs/potholes");
app.MapHealthChecks("/health");

app.Run();
