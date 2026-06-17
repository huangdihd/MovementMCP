package movementmcp.tools.system;

import movementmcp.McpTool;
import com.google.gson.JsonObject;
import xin.bbtt.mcbot.Bot;
import java.util.List;

public class SendCommandTool implements McpTool {
    public String name() { return "sendCommand"; }
    public String description() { return "Execute server command (without leading /)."; }
    public List<Param> params() { return List.of(Param.str("command", "Command text without /")); }
    public String execute(JsonObject args) {
        String cmd = args.get("command").getAsString();
        if (Bot.INSTANCE == null) return "Bot not ready.";
        Bot.INSTANCE.sendCommand(cmd);
        return "Command sent: " + cmd;
    }
}
