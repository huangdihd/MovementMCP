package movementmcp.tools.movement;

import static movementmcp.McpHelpers.*;
import movementmcp.McpTool;
import com.google.gson.JsonObject;
import org.joml.Vector3d;
import xin.bbtt.MovementSync;
import xin.bbtt.movements.WalkMovement;
import java.util.List;

public class WalkToTool implements McpTool {
    public String name() { return "walkTo"; }
    public String description() { return "Walk to absolute coordinates x, y, z. ~4.3 blocks/sec."; }
    public List<Param> params() { return List.of(Param.num("x"), Param.num("y"), Param.num("z")); }
    public String execute(JsonObject args) {
        double x = args.get("x").getAsDouble(), y = args.get("y").getAsDouble(), z = args.get("z").getAsDouble();
        if (!ready()) return "MovementSync not ready.";
        Vector3d cur = pos(), tgt = new Vector3d(x, y, z);
        double dist = cur.distance(tgt);
        if (dist < 0.1) return "Already at target.";
        Vector3d vel = new Vector3d(tgt).sub(cur).normalize().mul(MovementSync.movementSpeed);
        long ms = (long) ((dist / MovementSync.movementSpeed) * 50);
        MovementSync.INSTANCE.movementController.addMovement(new WalkMovement(vel, ms));
        return String.format("Walking to (%.2f, %.2f, %.2f), ETA %dms.", x, y, z, ms);
    }
}
