package movementmcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xin.bbtt.mcbot.plugin.Plugin;

public class MovementMcpPlugin implements Plugin {

    private static final Logger logger = LoggerFactory.getLogger(MovementMcpPlugin.class);
    public static MovementMcpPlugin INSTANCE;

    private MovementMcpServer mcpServer;
    private int port = 3000;

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
            logger.warn("[MovementMCP] Server is already running on port {}", port);
            return;
        }
        try {
            logger.info("[MovementMCP] Creating server on port {}...", port);
            mcpServer = new MovementMcpServer(port);
            logger.info("[MovementMCP] Sever created, starting...");
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

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
