using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.SignalR;

namespace PotholeDetection.Api.Hubs;

[Authorize]
public class PotholeHub : Hub
{
    // Clients connect and receive broadcasts pushed from PotholeService.
    // No client-invocable methods needed.
}
