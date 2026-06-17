package movementmcp.tools.movement;

import static movementmcp.McpHelpers.*;
import movementmcp.McpTool;
import com.google.gson.JsonObject;
import xin.bbtt.MovementSync;
import java.util.List;

public class JumpTool implements McpTool {
    public String name() { return "jump"; }
    public String description() { return "Jump. ~500ms."; }
    public List<Param> params() { return List.of(); }
    public String execute(JsonObject args) {
        if (!ready()) return "Plugin not ready.";
        MovementSync.INSTANCE.jump();
        return "Jumped.";
    }
}
