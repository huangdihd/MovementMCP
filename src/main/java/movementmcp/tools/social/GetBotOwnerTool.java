package movementmcp.tools.social;

import movementmcp.McpTool;
import com.google.gson.JsonObject;
import xin.bbtt.mcbot.Bot;
import java.util.List;

public class GetBotOwnerTool implements McpTool {
    public String name() { return "getBotOwner"; }
    public String description() { return "Get the bot owner name from config."; }
    public List<Param> params() { return List.of(); }
    public String execute(JsonObject args) {
        if (Bot.INSTANCE == null || Bot.INSTANCE.getConfig() == null) return "Cannot read config.";
        String owner = Bot.INSTANCE.getConfig().getConfigData().getOwner();
        return "Owner: " + (owner == null ? "Not set" : owner);
    }
}
