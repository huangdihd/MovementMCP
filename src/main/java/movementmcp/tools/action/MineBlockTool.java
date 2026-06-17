package movementmcp.tools.action;

import static movementmcp.McpHelpers.*;
import movementmcp.McpTool;
import com.google.gson.JsonObject;
import org.joml.Vector3d;
import xin.bbtt.MovementSync;
import xin.bbtt.movements.DigBlockMovement;
import java.util.List;

public class MineBlockTool implements McpTool {
    public String name() { return "mineBlock"; }
    public String description() { return "Mine block at coordinates."; }
    public List<Param> params() { return List.of(Param.integer("x"), Param.integer("y"), Param.integer("z")); }
    public String execute(JsonObject args) {
        int x = args.get("x").getAsInt(), y = args.get("y").getAsInt(), z = args.get("z").getAsInt();
        MovementSync.INSTANCE.lookAt(new Vector3d(x + 0.5, y + 0.5, z + 0.5));
        MovementSync.INSTANCE.getMovementController().addMovement(new DigBlockMovement(org.cloudburstmc.math.vector.Vector3i.from(x, y, z)));
        return "Mining (" + x + "," + y + "," + z + ")";
    }
}
