package movementmcp;

import xin.bbtt.mcbot.Bot;

import java.nio.charset.StandardCharsets;

public class SystemTools extends BaseMcpTools {

    @Override
    protected void init() {
        reg("sendChatMessage", "Send chat message visible to all.", args -> {
            String msg = args.get("message").getAsString();
            if (Bot.INSTANCE == null) return "Bot not ready.";
            sendChatInChunks(msg);
            return "Message sent.";
        }, str("message", "Message to send"));

        reg("sendCommand", "Execute server command (without leading /).", args -> {
            String cmd = args.get("command").getAsString();
            if (Bot.INSTANCE == null) return "Bot not ready.";
            Bot.INSTANCE.sendCommand(cmd);
            return "Command sent: " + cmd;
        }, str("command", "Command text without /"));
    }

    private static void sendChatInChunks(String text) {
        int limit = 90;
        StringBuilder chunk = new StringBuilder();
        int bytes = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int cb = String.valueOf(c).getBytes(StandardCharsets.UTF_8).length;
            if (bytes + cb > limit) {
                Bot.INSTANCE.sendChatMessage(chunk.toString());
                chunk = new StringBuilder();
                bytes = 0;
                try { Thread.sleep(250); } catch (InterruptedException ignored) {}
            }
            chunk.append(c);
            bytes += cb;
        }
        if (!chunk.isEmpty()) Bot.INSTANCE.sendChatMessage(chunk.toString());
    }
}
