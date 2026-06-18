package movementmcp.tools.social;

import movementmcp.McpTool;
import com.google.gson.JsonObject;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import xin.bbtt.mcbot.Bot;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GetRecentChatTool implements McpTool {

    static final List<String> messages = new CopyOnWriteArrayList<>();

    static {
        try {
            if (Bot.INSTANCE != null) {
                Bot.INSTANCE.getPluginManager().addListener(new SessionAdapter() {
                    @Override
                    public void packetReceived(Session session, Packet packet) {
                        if (packet instanceof ClientboundSystemChatPacket p) {
                            String text = p.getContent().toString();
                            if (text != null && !text.isBlank()) {
                                messages.add(text);
                                if (messages.size() > 200) messages.remove(0);
                            }
                        }
                    }
                }, null);
            }
        } catch (Exception ignored) {}
    }

    public String name() { return "getRecentChat"; }
    public String description() { return "Get recent public chat or system broadcast messages."; }
    public List<Param> params() { return List.of(Param.integer("lines", "Number of recent lines, max 100")); }
    public String execute(JsonObject args) {
        int lines = Math.min(100, args.get("lines").getAsInt());
        if (messages.isEmpty()) return "No recent chat messages.";
        int count = Math.min(lines, messages.size());
        List<String> sub = new ArrayList<>(messages.subList(messages.size() - count, messages.size()));
        return "Recent " + sub.size() + " messages:\n" + String.join("\n", sub);
    }
}
