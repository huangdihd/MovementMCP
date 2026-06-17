package movementmcp;

import com.google.gson.*;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class MovementMcpServer {

    private static final Logger logger = LoggerFactory.getLogger(MovementMcpServer.class);

    private final int port;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ToolBridge toolBridge = new ToolBridge();
    private final Gson gson = new GsonBuilder().create();

    private final List<SseConnection> sseConnections = new CopyOnWriteArrayList<>();
    private final Map<String, SseConnection> sessionConnections = new ConcurrentHashMap<>();
    private int sessionCounter = 0;

    public MovementMcpServer(int port) {
        this.port = port;
    }

    public void start() {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new HttpServerCodec());
                        p.addLast(new HttpObjectAggregator(65536));
                        p.addLast(new ChunkedWriteHandler());
                        p.addLast(new McpHttpHandler());
                    }
                });
        try {
            serverChannel = b.bind(port).sync().channel();
            running.set(true);
            logger.info("[MovementMCP] SSE server listening on http://0.0.0.0:{}", port);
        } catch (InterruptedException e) {
            logger.error("[MovementMCP] Bind interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    public void stop() {
        running.set(false);
        for (SseConnection conn : sseConnections) {
            conn.close();
        }
        sseConnections.clear();
        sessionConnections.clear();
        if (serverChannel != null) serverChannel.close();
        if (bossGroup != null) bossGroup.shutdownGracefully();
        if (workerGroup != null) workerGroup.shutdownGracefully();
    }

    public boolean isRunning() {
        return running.get();
    }

    private static class SseConnection {
        final ChannelHandlerContext ctx;
        final String sessionId;
        volatile boolean closed = false;
        volatile boolean headersSent = false;

        SseConnection(ChannelHandlerContext ctx, String sessionId) {
            this.ctx = ctx;
            this.sessionId = sessionId;
        }

        void sendEvent(String eventType, String data) {
            if (closed || !ctx.channel().isActive()) return;
            String frame = "event: " + eventType + "\ndata: " + data + "\n\n";
            ByteBuf buf = Unpooled.copiedBuffer(frame, StandardCharsets.UTF_8);
            if (!headersSent) {
                DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/event-stream");
                response.headers().set(HttpHeaderNames.CACHE_CONTROL, "no-cache");
                response.headers().set(HttpHeaderNames.CONNECTION, "keep-alive");
                response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
                ctx.writeAndFlush(response);
                headersSent = true;
            } else {
                ctx.writeAndFlush(buf);
            }
        }

        void sendEndpoint(String endpoint) {
            sendEvent("endpoint", endpoint);
        }

        void sendJsonRpc(String json) {
            sendEvent("message", json);
        }

        void close() {
            closed = true;
            try { ctx.close(); } catch (Exception ignored) {}
        }
    }

    private class McpHttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) {
            String uri = req.uri();
            String path = uri.contains("?") ? uri.substring(0, uri.indexOf('?')) : uri;

            if ("GET".equalsIgnoreCase(req.method().name()) && "/sse".equals(path)) {
                handleSse(ctx, req);
            } else if ("POST".equalsIgnoreCase(req.method().name()) && path.startsWith("/message")) {
                handleMessage(ctx, req);
            } else if ("OPTIONS".equalsIgnoreCase(req.method().name())) {
                handleOptions(ctx);
            } else {
                sendSimpleResponse(ctx, HttpResponseStatus.NOT_FOUND, "Not Found");
            }
        }

        private void handleSse(ChannelHandlerContext ctx, FullHttpRequest req) {
            String sessionId = "session-" + (++sessionCounter);
            SseConnection conn = new SseConnection(ctx, sessionId);
            sseConnections.add(conn);
            sessionConnections.put(sessionId, conn);
            logger.info("[MovementMCP] New SSE connection: {}", sessionId);

            String endpoint = "/message?sessionId=" + sessionId;
            conn.sendEndpoint(endpoint);
        }

        private void handleMessage(ChannelHandlerContext ctx, FullHttpRequest req) {
            String uri = req.uri();
            String sessionId = null;
            if (uri.contains("sessionId=")) {
                int idx = uri.indexOf("sessionId=");
                String rest = uri.substring(idx + 10);
                int amp = rest.indexOf('&');
                sessionId = amp >= 0 ? rest.substring(0, amp) : rest;
            }

            String body = req.content().toString(StandardCharsets.UTF_8);
            logger.debug("[MovementMCP] Received message: {}", body);

            // Send 202 Accepted
            DefaultFullHttpResponse ack = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.ACCEPTED);
            ack.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
            ack.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            ctx.writeAndFlush(ack);

            SseConnection conn = sessionId != null ? sessionConnections.get(sessionId) : null;
            if (conn == null && !sseConnections.isEmpty()) {
                conn = sseConnections.get(sseConnections.size() - 1);
            }
            if (conn != null) {
                processJsonRpc(body, conn);
            }
        }

        private void handleOptions(ChannelHandlerContext ctx) {
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, OPTIONS");
            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
            ctx.writeAndFlush(response);
        }

        private void sendSimpleResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String msg) {
            ByteBuf buf = Unpooled.copiedBuffer(msg, StandardCharsets.UTF_8);
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, status, buf);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, buf.readableBytes());
            ctx.writeAndFlush(response);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.error("[MovementMCP] Channel exception", cause);
            sseConnections.removeIf(c -> c.ctx == ctx);
            ctx.close();
        }
    }

    private void processJsonRpc(String body, SseConnection conn) {
        JsonObject request;
        try {
            request = JsonParser.parseString(body).getAsJsonObject();
        } catch (Exception e) {
            logger.error("[MovementMCP] Invalid JSON-RPC", e);
            return;
        }

        String method = request.has("method") ? request.get("method").getAsString() : null;
        JsonElement idElement = request.has("id") ? request.get("id") : null;
        JsonElement params = request.has("params") ? request.get("params") : null;

        if (method == null) return;
        boolean isNotification = idElement == null;

        logger.info("[MovementMCP] Method: {}", method);

        switch (method) {
            case "initialize" -> {
                JsonObject result = new JsonObject();
                result.addProperty("protocolVersion", "2024-11-05");
                JsonObject capabilities = new JsonObject();
                capabilities.add("tools", new JsonObject());
                result.add("capabilities", capabilities);
                JsonObject serverInfo = new JsonObject();
                serverInfo.addProperty("name", "XinMCP");
                serverInfo.addProperty("version", "1.0.0");
                result.add("serverInfo", serverInfo);
                sendResponse(conn, idElement, result);
            }
            case "notifications/initialized" -> {}
            case "tools/list" -> {
                JsonObject result = new JsonObject();
                result.add("tools", toolBridge.listTools());
                sendResponse(conn, idElement, result);
            }
            case "tools/call" -> {
                String toolName = params != null && params.getAsJsonObject().has("name")
                        ? params.getAsJsonObject().get("name").getAsString() : null;
                JsonObject arguments = params != null && params.getAsJsonObject().has("arguments")
                        ? params.getAsJsonObject().get("arguments").getAsJsonObject() : new JsonObject();

                if (toolName == null) {
                    sendError(conn, idElement, -32602, "Missing tool name");
                    return;
                }
                try {
                    String resultText = toolBridge.callTool(toolName, arguments);
                    JsonObject result = new JsonObject();
                    JsonArray content = new JsonArray();
                    JsonObject textContent = new JsonObject();
                    textContent.addProperty("type", "text");
                    textContent.addProperty("text", resultText);
                    content.add(textContent);
                    result.add("content", content);
                    sendResponse(conn, idElement, result);
                } catch (Exception e) {
                    logger.error("[MovementMCP] Tool error: {}", toolName, e);
                    JsonObject result = new JsonObject();
                    JsonArray content = new JsonArray();
                    JsonObject textContent = new JsonObject();
                    textContent.addProperty("type", "text");
                    textContent.addProperty("text", "Error: " + e.getMessage());
                    content.add(textContent);
                    result.add("content", content);
                    result.addProperty("isError", true);
                    sendResponse(conn, idElement, result);
                }
            }
            default -> {
                if (!isNotification) {
                    sendError(conn, idElement, -32601, "Method not found: " + method);
                }
            }
        }
    }

    private void sendResponse(SseConnection conn, JsonElement id, JsonObject result) {
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        response.add("id", id);
        response.add("result", result);
        conn.sendJsonRpc(gson.toJson(response));
    }

    private void sendError(SseConnection conn, JsonElement id, int code, String message) {
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        response.add("id", id);
        JsonObject error = new JsonObject();
        error.addProperty("code", code);
        error.addProperty("message", message);
        response.add("error", error);
        conn.sendJsonRpc(gson.toJson(response));
    }
}
