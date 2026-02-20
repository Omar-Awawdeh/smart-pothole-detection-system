FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build
WORKDIR /src

COPY src/PotholeDetection.Api/PotholeDetection.Api.csproj src/PotholeDetection.Api/
RUN dotnet restore src/PotholeDetection.Api/PotholeDetection.Api.csproj

COPY . .
RUN dotnet publish src/PotholeDetection.Api/PotholeDetection.Api.csproj \
    -c Release -o /app/publish --no-restore

FROM mcr.microsoft.com/dotnet/aspnet:8.0
WORKDIR /app

RUN mkdir -p /app/wwwroot/uploads

COPY --from=build /app/publish .

ENV ASPNETCORE_ENVIRONMENT=Development
ENV ASPNETCORE_URLS=http://+:8080

EXPOSE 8080

ENTRYPOINT ["dotnet", "PotholeDetection.Api.dll"]
