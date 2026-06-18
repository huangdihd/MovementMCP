package movementmcp.trackers;

import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRespawnPacket;
import xin.bbtt.mcbot.event.EventHandler;
import xin.bbtt.mcbot.event.Listener;
import xin.bbtt.mcbot.events.ReceivePacketEvent;

public class DimensionTracker implements Listener {

    private static String currentDimension = "Unknown";

    @EventHandler
    public void onLogin(ReceivePacketEvent<ClientboundLoginPacket> event) {
        currentDimension = event.getPacket().getCommonPlayerSpawnInfo().getWorldName().asString();
    }

    @EventHandler
    public void onRespawn(ReceivePacketEvent<ClientboundRespawnPacket> event) {
        currentDimension = event.getPacket().getCommonPlayerSpawnInfo().getWorldName().asString();
    }

    public static String getCurrentDimension() {
        return currentDimension;
    }
}
