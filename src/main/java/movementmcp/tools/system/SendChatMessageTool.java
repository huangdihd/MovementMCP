package movementmcp.tools.system;

import movementmcp.McpTool;
import com.google.gson.JsonObject;
import xin.bbtt.mcbot.Bot;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class SendChatMessageTool implements McpTool {
    public String name() { return "sendChatMessage"; }
    public String description() { return "Send chat message visible to all."; }
    public List<Param> params() { return List.of(Param.str("message", "Message to send")); }
    public String execute(JsonObject args) {
        String msg = args.get("message").getAsString();
        if (Bot.INSTANCE == null) return "Bot not ready.";
        if (msg.matches(".*?-?\\d{2,}[\\s,]+-?\\d{1,}[\\s,]+-?\\d{2,}.*")) {
            return "Blocked: message contains suspected coordinates. Do not leak coordinates!";
        }
        sendInChunks(msg);
        return "Message sent.";
    }
    private static void sendInChunks(String text) {
        int limit = 90; StringBuilder chunk = new StringBuilder(); int bytes = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int cb = String.valueOf(c).getBytes(StandardCharsets.UTF_8).length;
            if (bytes + cb > limit) { Bot.INSTANCE.sendChatMessage(chunk.toString()); chunk = new StringBuilder(); bytes = 0; try { Thread.sleep(250); } catch (InterruptedException ignored) {} }
            chunk.append(c); bytes += cb;
        }
        if (!chunk.isEmpty()) Bot.INSTANCE.sendChatMessage(chunk.toString());
    }
}
