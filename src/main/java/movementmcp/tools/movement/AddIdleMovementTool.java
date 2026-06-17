package movementmcp.tools.movement;

import static movementmcp.McpHelpers.*;
import movementmcp.McpTool;
import com.google.gson.JsonObject;
import xin.bbtt.MovementSync;
import xin.bbtt.movements.ActionMovement;
import java.util.List;

public class AddIdleMovementTool implements McpTool {
    public String name() { return "addIdleMovement"; }
    public String description() { return "Add idle wait to movement queue (ms)."; }
    public List<Param> params() { return List.of(Param.integer("durationMs", "Wait duration in milliseconds")); }
    public String execute(JsonObject args) {
        long ms = args.get("durationMs").getAsLong();
        if (!ready()) return "MovementSync not ready.";
        MovementSync.INSTANCE.movementController.addMovement(new ActionMovement(() -> {}, ms));
        return "Idle " + ms + "ms added.";
    }
}
