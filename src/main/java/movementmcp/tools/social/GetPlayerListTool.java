package movementmcp.tools.social;

import movementmcp.McpTool;
import com.google.gson.JsonObject;
import xin.bbtt.mcbot.Bot;
import java.util.List;

public class GetPlayerListTool implements McpTool {
    public String name() { return "getPlayerList"; }
    public String description() { return "List online players."; }
    public List<Param> params() { return List.of(); }
    public String execute(JsonObject args) {
        if (Bot.INSTANCE == null || Bot.INSTANCE.players == null || Bot.INSTANCE.players.isEmpty())
            return "No player info.";
        var names = Bot.INSTANCE.players.values().stream().map(org.geysermc.mcprotocollib.auth.GameProfile::getName).toList();
        return "Online (" + names.size() + "): " + String.join(", ", names);
    }
}
