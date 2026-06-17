package movementmcp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ToolBridge {

    private static final Logger logger = LoggerFactory.getLogger(ToolBridge.class);
    private final Map<String, McpTool> toolMap = new LinkedHashMap<>();

    public ToolBridge() {
        registerAll(
            new movementmcp.tools.movement.WalkToTool(), new movementmcp.tools.movement.LookAtTool(), new movementmcp.tools.movement.JumpTool(),
            new movementmcp.tools.movement.PathfindToTool(), new movementmcp.tools.movement.StopWalkingTool(), new movementmcp.tools.movement.GetMovementStatusTool(),
            new movementmcp.tools.movement.AddIdleMovementTool(),

            new movementmcp.tools.perception.WhereAmITool(), new movementmcp.tools.perception.ScanSurroundingsTool(),
            new movementmcp.tools.perception.GetAreaMapTool(), new movementmcp.tools.perception.GetNearbyEntitiesTool(),
            new movementmcp.tools.perception.GetBlocksInCubeTool(), new movementmcp.tools.perception.FindSpecificBlocksTool(),

            new movementmcp.tools.action.InteractEntityTool(), new movementmcp.tools.action.ChangeSlotTool(),
            new movementmcp.tools.action.UseItemTool(), new movementmcp.tools.action.UseItemWithDurationTool(),
            new movementmcp.tools.action.ReleaseUseItemTool(), new movementmcp.tools.action.InteractBlockTool(),
            new movementmcp.tools.action.MineBlockTool(),

            new movementmcp.tools.social.GetPlayerListTool(), new movementmcp.tools.social.GetNearbyPlayersTool(),
            new movementmcp.tools.social.FollowPlayerTool(), new movementmcp.tools.social.StopFollowingTool(),

            new movementmcp.tools.system.SendChatMessageTool(), new movementmcp.tools.system.SendCommandTool()
        );
        logger.info("[MovementMCP] Registered {} tools", toolMap.size());
    }

    private void registerAll(McpTool... toolList) {
        for (McpTool t : toolList) toolMap.put(t.name(), t);
    }

    public JsonArray listTools() {
        JsonArray arr = new JsonArray();
        for (McpTool t : toolMap.values()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("name", t.name());
            obj.addProperty("description", t.description());
            obj.add("inputSchema", buildSchema(t.params()));
            arr.add(obj);
        }
        return arr;
    }

    public String callTool(String name, JsonObject args) throws Exception {
        McpTool t = toolMap.get(name);
        if (t == null) throw new IllegalArgumentException("Unknown tool: " + name);
        return t.execute(args);
    }

    private static JsonObject buildSchema(List<McpTool.Param> params) {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject props = new JsonObject();
        JsonArray req = new JsonArray();
        for (McpTool.Param p : params) {
            JsonObject s = new JsonObject();
            s.addProperty("type", p.type());
            s.addProperty("description", p.desc());
            props.add(p.name(), s);
            req.add(p.name());
        }
        schema.add("properties", props);
        schema.add("required", req);
        return schema;
    }
}
