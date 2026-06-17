package movementmcp.tools.movement;

import static movementmcp.McpHelpers.*;
import movementmcp.McpTool;
import com.google.gson.JsonObject;
import org.joml.Vector3d;
import org.joml.Vector3i;
import xin.bbtt.MovementSync;
import xin.bbtt.pathfinding.DStarLite;
import xin.bbtt.pathfinding.Node;
import java.util.List;

public class PathfindToTool implements McpTool {
    public String name() { return "pathfindTo"; }
    public String description() { return "Pathfind to x, y, z with auto obstacle handling."; }
    public List<Param> params() { return List.of(Param.num("x"), Param.num("y"), Param.num("z")); }
    public String execute(JsonObject args) {
        double x = args.get("x").getAsDouble(), y = args.get("y").getAsDouble(), z = args.get("z").getAsDouble();
        if (!ready()) return "MovementSync not ready.";
        Vector3d p = pos();
        Node start = new Node((int) Math.floor(p.x), (int) Math.floor(p.y), (int) Math.floor(p.z));
        Node goal = new Node((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
        List<Node> path = new DStarLite(start, goal, MovementSync.INSTANCE.getWorld()).findPath(2000);
        if (path == null || path.size() <= 1)
            return String.format("Pathfinding failed to (%.1f, %.1f, %.1f).", x, y, z);
        MovementSync.INSTANCE.setActiveGoal(new Vector3i((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z)));
        MovementSync.INSTANCE.triggerAutoRepath();
        return String.format("Pathfinding started (%d nodes) to (%.2f, %.2f, %.2f).", path.size(), x, y, z);
    }
}
