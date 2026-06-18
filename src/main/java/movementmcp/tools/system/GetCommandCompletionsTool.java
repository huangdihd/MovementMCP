package movementmcp.tools.system;

import movementmcp.McpTool;
import com.google.gson.JsonObject;
import xin.bbtt.mcbot.Bot;
import java.util.List;

public class GetCommandCompletionsTool implements McpTool {
    public String name() { return "getCommandCompletions"; }
    public String description() { return "Get autocomplete suggestions for a command."; }
    public List<Param> params() { return List.of(Param.str("partialCommand", "Partial command text without /")); }
    public String execute(JsonObject args) {
        String cmd = args.get("partialCommand").getAsString();
        if (Bot.INSTANCE == null || Bot.INSTANCE.getPluginManager() == null) return "Bot not ready.";
        List<String> completions = Bot.INSTANCE.getPluginManager().commands().callComplete(cmd);
        if (completions == null || completions.isEmpty()) return "No completions found.";
        return "Completions: " + String.join(", ", completions);
    }
}
