package movementmcp.tools.movement;

import static movementmcp.McpHelpers.*;
import movementmcp.McpTool;
import com.google.gson.JsonObject;
import org.joml.Vector3d;
import xin.bbtt.MovementSync;
import java.util.List;

public class LookAtTool implements McpTool {
    public String name() { return "lookAt"; }
    public String description() { return "Look at coordinates x, y, z. Near-instant."; }
    public List<Param> params() { return List.of(Param.num("x"), Param.num("y"), Param.num("z")); }
    public String execute(JsonObject args) {
        double x = args.get("x").getAsDouble(), y = args.get("y").getAsDouble(), z = args.get("z").getAsDouble();
        if (!ready()) return "Plugin not ready.";
        MovementSync.INSTANCE.lookAt(new Vector3d(x, y, z));
        return String.format("Looking at (%.2f, %.2f, %.2f).", x, y, z);
    }
}
