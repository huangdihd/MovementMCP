package movementmcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xin.bbtt.mcbot.plugin.Plugin;

public class MovementMcpPlugin implements Plugin {

    private static final Logger logger = LoggerFactory.getLogger(MovementMcpPlugin.class);
    public static MovementMcpPlugin INSTANCE;

    private MovementMcpServer mcpServer;
    private int port = 6735;

    @Override
    public void onLoad() {
        logger.info("[MovementMCP] Loading...");
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        logger.info("[MovementMCP] Enabling...");
        INSTANCE = this;
        new Thread(this::startServer, "mcp-start").start();
        try {
            xin.bbtt.mcbot.Bot.INSTANCE.getPluginManager().events().registerEvents(new movementmcp.listeners.AgentWakeupListener(), this);
            xin.bbtt.mcbot.Bot.INSTANCE.getPluginManager().events().registerEvents(new movementmcp.trackers.DimensionTracker(), this);
            logger.info("[MovementMCP] Registered AgentWakeupListener and DimensionTracker.");
        } catch (Exception e) {
            logger.error("[MovementMCP] Failed to register listeners", e);
        }
    }

    @Override
    public void onDisable() {
        logger.info("[MovementMCP] Disabling...");
        stopServer();
    }

    @Override
    public void onUnload() {
        logger.info("[MovementMCP] Unloading...");
    }

    public void startServer() {
        if (mcpServer != null && mcpServer.isRunning()) {
            logger.warn("[MovementMCP] Server already running on port {}", port);
            return;
        }
        try {
            mcpServer = new MovementMcpServer(port);
            mcpServer.start();
            logger.info("[MovementMCP] MCP Server started on port {}", port);
        } catch (Exception e) {
            logger.error("[MovementMCP] Failed to start MCP server", e);
        }
    }

    public void stopServer() {
        if (mcpServer != null) {
            mcpServer.stop();
            mcpServer = null;
            logger.info("[MovementMCP] MCP Server stopped");
        }
    }

    public boolean isRunning() {
        return mcpServer != null && mcpServer.isRunning();
    }

    public void sendNotification(String method, com.google.gson.JsonObject params) {
        if (mcpServer != null) {
            mcpServer.broadcastNotification(method, params);
        }
    }

    public void sendMcpMessage(String level, String loggerName, Object data) {
        if (mcpServer != null) {
            com.google.gson.JsonObject params = new com.google.gson.JsonObject();
            params.addProperty("level", level);
            if (loggerName != null) {
                params.addProperty("logger", loggerName);
            }
            if (data instanceof com.google.gson.JsonElement) {
                params.add("data", (com.google.gson.JsonElement) data);
            } else if (data instanceof String) {
                params.addProperty("data", (String) data);
            } else if (data != null) {
                params.addProperty("data", data.toString());
            }
            mcpServer.broadcastNotification("notifications/message", params);
        }
    }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
}
