package movementmcp.tools.perception;

import static movementmcp.McpHelpers.*;
import movementmcp.McpTool;
import com.google.gson.JsonObject;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundSetHealthPacket;
import xin.bbtt.mcbot.Bot;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class GetVitalsTool implements McpTool {

    static final AtomicReference<float[]> vitals = new AtomicReference<>(new float[]{20f, 20f, 5f});

    static {
        try {
            if (Bot.INSTANCE != null) {
                Bot.INSTANCE.getPluginManager().addListener(new SessionAdapter() {
                    @Override
                    public void packetReceived(Session session, Packet packet) {
                        if (packet instanceof ClientboundSetHealthPacket p) {
                            vitals.set(new float[]{p.getHealth(), (float) p.getFood(), p.getSaturation()});
                        }
                    }
                }, null);
            }
        } catch (Exception ignored) {}
    }

    public String name() { return "getVitals"; }
    public String description() { return "Get health, hunger, saturation vitals."; }
    public List<Param> params() { return List.of(); }
    public String execute(JsonObject args) {
        float[] v = vitals.get();
        float hp = v[0]; float food = v[1]; float sat = v[2];
        String status = hp <= 0 ? " [DEAD]" : hp < 6 ? " [CRITICAL]" : food <= 6 ? " [HUNGRY]" : "";
        return String.format("HP: %.1f/20, Food: %.0f/20, Saturation: %.1f%s", hp, food, sat, status);
    }
}
