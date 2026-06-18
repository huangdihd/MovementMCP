package movementmcp.tools.perception;

import static movementmcp.McpHelpers.*;
import movementmcp.McpTool;
import com.google.gson.JsonObject;
import org.joml.Vector3d;
import xin.bbtt.MovementSync;
import xin.bbtt.Entity.Entity;
import java.util.*;

public class GetNearbyEntitiesTool implements McpTool {
    public String name() { return "getNearbyEntities"; }
    public String description() { return "List entities within radius."; }
    public List<Param> params() { return List.of(Param.num("radius", "Search radius in blocks")); }
    public String execute(JsonObject args) {
        double radius = args.get("radius").getAsDouble();
        if (!ready() || MovementSync.INSTANCE.getWorld() == null) return "World not available.";
        Vector3d cur = pos();
        try {
            var field = xin.bbtt.world.World.class.getDeclaredField("entities");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<Integer, Entity> entities = (Map<Integer, Entity>) field.get(MovementSync.INSTANCE.getWorld());
            if (entities == null || entities.isEmpty()) return "No entities cached.";
            List<Entity> nearby = new ArrayList<>();
            for (Entity e : entities.values())
                if (e != null && e.getPosition() != null && e.getEntityId() != MovementSync.INSTANCE.entityId && cur.distance(e.getPosition()) <= radius) nearby.add(e);
            if (nearby.isEmpty()) return "No entities within " + radius + " blocks.";
            nearby.sort(Comparator.comparingDouble(e -> cur.distance(e.getPosition())));
            StringBuilder sb = new StringBuilder(String.format("Found %d entities:\n", nearby.size()));
            for (int i = 0; i < Math.min(nearby.size(), 40); i++) {
                Entity e = nearby.get(i);
                sb.append(String.format("  [%s] ID:%d dist:%.1f pos:(%.1f,%.1f,%.1f)\n", e.getType().name(), e.getEntityId(), cur.distance(e.getPosition()), e.getPosition().x, e.getPosition().y, e.getPosition().z));
            }
            return sb.toString();
        } catch (Exception e) { return "Error: " + e.getMessage(); }
    }
}
