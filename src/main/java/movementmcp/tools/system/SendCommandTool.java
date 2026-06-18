package movementmcp.tools.system;

import movementmcp.McpTool;
import com.google.gson.JsonObject;
import xin.bbtt.mcbot.Bot;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class SendCommandTool implements McpTool {
    public String name() { return "sendCommand"; }
    public String description() { return "Execute server command (without leading /)."; }
    public List<Param> params() { return List.of(Param.str("command", "Command text without /")); }
    public String execute(JsonObject args) {
        String cmd = args.get("command").getAsString();
        if (Bot.INSTANCE == null) return "Bot not ready.";
        if (cmd.startsWith("tell ") || cmd.startsWith("msg ") || cmd.startsWith("w ")) {
            String[] parts = cmd.split(" ", 3);
            if (parts.length == 3) {
                sendTellInChunks(parts[0], parts[1], parts[2]);
                return "Command sent (chunked).";
            }
        }
        Bot.INSTANCE.sendCommand(cmd);
        return "Command sent: " + cmd;
    }
    private static void sendTellInChunks(String cmdType, String recipient, String text) {
        int limit = 80; StringBuilder chunk = new StringBuilder(); int bytes = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int cb = String.valueOf(c).getBytes(StandardCharsets.UTF_8).length;
            if (bytes + cb > limit) { Bot.INSTANCE.sendCommand(cmdType + " " + recipient + " " + chunk); chunk = new StringBuilder(); bytes = 0; try { Thread.sleep(250); } catch (InterruptedException ignored) {} }
            chunk.append(c); bytes += cb;
        }
        if (!chunk.isEmpty()) Bot.INSTANCE.sendCommand(cmdType + " " + recipient + " " + chunk);
    }
}
