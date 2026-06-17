package movementmcp.tools.movement;

import static movementmcp.McpHelpers.*;
import movementmcp.McpTool;
import com.google.gson.JsonObject;
import xin.bbtt.MovementSync;
import java.util.List;

public class StopWalkingTool implements McpTool {
    public String name() { return "stopWalking"; }
    public String description() { return "Stop all movement and pathfinding."; }
    public List<Param> params() { return List.of(); }
    public String execute(JsonObject args) {
        if (!ready()) return "MovementSync not ready.";
        MovementSync.INSTANCE.movementController.cancelAll();
        return "All movement stopped.";
    }
}
