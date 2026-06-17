package movementmcp.tools.perception;

import static movementmcp.McpHelpers.*;
import movementmcp.McpTool;
import com.google.gson.JsonObject;
import org.joml.Vector3d;
import xin.bbtt.MovementSync;
import xin.bbtt.mcbot.Bot;
import java.util.List;

public class WhereAmITool implements McpTool {
    public String name() { return "whereAmI"; }
    public String description() { return "Get current position, server, facing direction."; }
    public List<Param> params() { return List.of(); }
    public String execute(JsonObject args) {
        if (!ready()) return "Plugin not ready.";
        Vector3d p = new Vector3d(pos());
        float yaw = MovementSync.INSTANCE.yaw.get() % 360;
        if (yaw < 0) yaw += 360;
        String facing = yaw < 45 || yaw >= 315 ? "South(+Z)" : yaw < 135 ? "West(-X)" : yaw < 225 ? "North(-Z)" : "East(+X)";
        return String.format("Server: %s, Pos: (%.2f, %.2f, %.2f), Facing: %s", Bot.INSTANCE.getServer(), p.x, p.y, p.z, facing);
    }
}
