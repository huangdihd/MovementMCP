package movementmcp.listeners;

import org.joml.Vector3d;
import xin.bbtt.events.DeathEvent;
import xin.bbtt.events.TeleportEvent;
import xin.bbtt.mcbot.Bot;
import xin.bbtt.mcbot.Utils;
import xin.bbtt.mcbot.event.EventHandler;
import xin.bbtt.mcbot.event.Listener;
import xin.bbtt.mcbot.events.PrivateChatEvent;
import movementmcp.MovementMcpPlugin;

public class AgentWakeupListener implements Listener {
    private long lastTeleportNotifyTime = 0;
    private long lastDeathNotifyTime = 0;

    @EventHandler
    public void onPrivateChat(PrivateChatEvent event) {
        if (Bot.INSTANCE == null || Bot.INSTANCE.getConfig() == null) return;
        String owner = Bot.INSTANCE.getConfig().getConfigData().getOwner();
        if (owner == null || owner.isEmpty()) return;
        
        String senderName = event.getSender().getName();
        if (!senderName.equalsIgnoreCase(owner)) return;

        String message = event.getMessage();
        MovementMcpPlugin.INSTANCE.sendMcpMessage("info", "WakeupEvent", 
            "Private message from owner " + senderName + ": " + message);
    }

    @EventHandler
    public void onDeath(DeathEvent event) {
        long now = System.currentTimeMillis();
        if (now - lastDeathNotifyTime < 15000) return;
        lastDeathNotifyTime = now;

        String deathMessage = event.getMessage() == null ? "(No death message)" : Utils.toString(event.getMessage());
        String msg = String.format("[SYSTEM_EVENT] You died! Message: %s. " +
                "System auto-requested respawn. " +
                "Your items dropped near the death location. " +
                "All pathfinding cancelled.", deathMessage);
        
        MovementMcpPlugin.INSTANCE.sendMcpMessage("warning", "WakeupEvent", msg);
    }

    @EventHandler
    public void onTeleport(TeleportEvent event) {
        Vector3d pos = event.getPosition();
        if (pos != null) {
            long now = System.currentTimeMillis();
            if (now - lastTeleportNotifyTime > 15000) {
                lastTeleportNotifyTime = now;
                String msg = String.format("[SYSTEM_EVENT] Server teleported (or pulled back) you to: (%.1f, %.1f, %.1f). " +
                        "This could be due to respawning, teleport command, or getting stuck. " +
                        "If you were pathfinding, check if you need to stopWalking or mine blocks.", pos.x, pos.y, pos.z);
                
                MovementMcpPlugin.INSTANCE.sendMcpMessage("warning", "WakeupEvent", msg);
            }
        }
    }
}
