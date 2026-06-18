package movementmcp.tools.perception;

import static movementmcp.McpHelpers.*;
import movementmcp.McpTool;
import movementmcp.trackers.DimensionTracker;
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
        float pitch = MovementSync.INSTANCE.pitch.get();
        String facing = yaw < 45 || yaw >= 315 ? "South(+Z)" : yaw < 135 ? "West(-X)" : yaw < 225 ? "North(-Z)" : "East(+X)";
        String pitchDir = pitch < -45 ? "Looking up" : (pitch > 45 ? "Looking down" : "Level");
        String dimension = DimensionTracker.getCurrentDimension();
        String standingOn = blockNameAt((int) Math.floor(p.x), (int) Math.floor(p.y) - 1, (int) Math.floor(p.z));
        return String.format("Server: %s, Dimension: %s, Pos: (%.2f, %.2f, %.2f), Facing: %s, %s (Yaw: %.1f, Pitch: %.1f), Standing on: %s",
                Bot.INSTANCE.getServer(), dimension, p.x, p.y, p.z, facing, pitchDir, yaw, pitch, standingOn);
    }
}
