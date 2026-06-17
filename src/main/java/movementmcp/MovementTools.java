package movementmcp;

import org.joml.Vector3d;
import org.joml.Vector3i;
import xin.bbtt.MovementSync;
import xin.bbtt.movements.WalkMovement;
import xin.bbtt.movements.ActionMovement;
import xin.bbtt.pathfinding.DStarLite;
import xin.bbtt.pathfinding.Node;

import java.util.List;

public class MovementTools extends BaseMcpTools {

    @Override
    protected void init() {
        reg("walkTo", "Walk to absolute coordinates x, y, z. ~4.3 blocks/sec.", args -> {
            double x = args.get("x").getAsDouble(), y = args.get("y").getAsDouble(), z = args.get("z").getAsDouble();
            if (!ready()) return "MovementSync not ready.";
            Vector3d cur = pos(), tgt = new Vector3d(x, y, z);
            double dist = cur.distance(tgt);
            if (dist < 0.1) return "Already at target.";
            Vector3d vel = new Vector3d(tgt).sub(cur).normalize().mul(MovementSync.movementSpeed);
            long ms = (long) ((dist / MovementSync.movementSpeed) * 50);
            MovementSync.INSTANCE.movementController.addMovement(new WalkMovement(vel, ms));
            return String.format("Walking to (%.2f, %.2f, %.2f), ETA %dms.", x, y, z, ms);
        }, num("x"), num("y"), num("z"));

        reg("lookAt", "Look at coordinates x, y, z. Near-instant.", args -> {
            double x = args.get("x").getAsDouble(), y = args.get("y").getAsDouble(), z = args.get("z").getAsDouble();
            if (!ready()) return "Plugin not ready.";
            MovementSync.INSTANCE.lookAt(new Vector3d(x, y, z));
            return String.format("Looking at (%.2f, %.2f, %.2f).", x, y, z);
        }, num("x"), num("y"), num("z"));

        reg("jump", "Jump. ~500ms.", args -> {
            if (!ready()) return "Plugin not ready.";
            MovementSync.INSTANCE.jump();
            return "Jumped.";
        });

        reg("pathfindTo", "Pathfind to x, y, z with auto obstacle handling.", args -> {
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
        }, num("x"), num("y"), num("z"));

        reg("stopWalking", "Stop all movement and pathfinding.", args -> {
            if (!ready()) return "MovementSync not ready.";
            MovementSync.INSTANCE.movementController.cancelAll();
            return "All movement stopped.";
        });

        reg("getMovementStatus", "Get current movement/pathfinding status.", args -> {
            if (!ready()) return "MovementSync not ready.";
            StringBuilder sb = new StringBuilder();
            Vector3i goal = MovementSync.INSTANCE.getActiveGoal();
            sb.append(goal != null ? String.format("Active goal: (%d, %d, %d).", goal.x, goal.y, goal.z) : "No active goal.");
            if (MovementSync.INSTANCE.movementController != null) {
                var cur = MovementSync.INSTANCE.movementController.getCurrentMovement();
                sb.append(cur != null ? "\nMoving (" + cur.getClass().getSimpleName() + ")." : "\nStationary.");
            }
            return sb.toString();
        });

        reg("addIdleMovement", "Add idle wait to movement queue (durationMs).", args -> {
            long ms = args.get("durationMs").getAsLong();
            if (!ready()) return "MovementSync not ready.";
            MovementSync.INSTANCE.movementController.addMovement(new ActionMovement(() -> {}, ms));
            return "Idle " + ms + "ms added.";
        }, integer("durationMs", "Wait duration in milliseconds"));
    }
}
