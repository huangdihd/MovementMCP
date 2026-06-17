package movementmcp.tools.movement;

import static movementmcp.McpHelpers.*;
import movementmcp.McpTool;
import com.google.gson.JsonObject;
import org.joml.Vector3i;
import xin.bbtt.MovementSync;
import java.util.List;

public class GetMovementStatusTool implements McpTool {
    public String name() { return "getMovementStatus"; }
    public String description() { return "Get current movement/pathfinding status."; }
    public List<Param> params() { return List.of(); }
    public String execute(JsonObject args) {
        if (!ready()) return "MovementSync not ready.";
        StringBuilder sb = new StringBuilder();
        Vector3i goal = MovementSync.INSTANCE.getActiveGoal();
        sb.append(goal != null ? String.format("Active goal: (%d, %d, %d).", goal.x, goal.y, goal.z) : "No active goal.");
        if (MovementSync.INSTANCE.movementController != null) {
            var cur = MovementSync.INSTANCE.movementController.getCurrentMovement();
            sb.append(cur != null ? "\nMoving (" + cur.getClass().getSimpleName() + ")." : "\nStationary.");
        }
        return sb.toString();
    }
}
