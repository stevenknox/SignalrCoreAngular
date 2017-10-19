using System.Threading.Tasks;
using Microsoft.AspNetCore.SignalR;

namespace signalrcorespa.Hubs
{
    public class LoopyHub : Hub
    {
        public Task Send(string data)
        {
            return Clients.All.InvokeAsync("Send", data);
        }
    }
}