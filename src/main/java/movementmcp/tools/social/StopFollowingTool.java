package movementmcp.tools.social;

import static movementmcp.McpHelpers.*;
import movementmcp.McpTool;
import com.google.gson.JsonObject;
import xin.bbtt.MovementSync;
import java.util.List;

public class StopFollowingTool implements McpTool {
    public String name() { return "stopFollowing"; }
    public String description() { return "Stop following."; }
    public List<Param> params() { return List.of(); }
    public String execute(JsonObject args) {
        if (!ready()) return "MovementSync not ready.";
        MovementSync.INSTANCE.setFollowTargetId(-1);
        MovementSync.INSTANCE.getMovementController().cancelAll();
        return "Stopped following.";
    }
}
