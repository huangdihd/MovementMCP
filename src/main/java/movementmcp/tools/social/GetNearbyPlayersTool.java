package movementmcp.tools.social;

import static movementmcp.McpHelpers.*;
import movementmcp.McpTool;
import com.google.gson.JsonObject;
import org.joml.Vector3d;
import xin.bbtt.MovementSync;
import xin.bbtt.mcbot.Bot;
import xin.bbtt.Entity.Entity;
import java.util.*;

public class GetNearbyPlayersTool implements McpTool {
    public String name() { return "getNearbyPlayers"; }
    public String description() { return "List nearby players with details."; }
    public List<Param> params() { return List.of(Param.num("radius", "Search radius")); }
    public String execute(JsonObject args) {
        double radius = args.get("radius").getAsDouble();
        if (!ready() || MovementSync.INSTANCE.getWorld() == null) return "World not available.";
        Vector3d cur = pos();
        try {
            var field = xin.bbtt.world.World.class.getDeclaredField("entities"); field.setAccessible(true);
            @SuppressWarnings("unchecked") Map<Integer, Entity> entities = (Map<Integer, Entity>) field.get(MovementSync.INSTANCE.getWorld());
            if (entities == null) return "No entities.";
            StringBuilder sb = new StringBuilder(); int count = 0;
            for (Entity e : entities.values()) {
                if (e == null || e.getPosition() == null) continue;
                if (e.getType() == org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType.PLAYER && MovementSync.INSTANCE.entityId != e.getEntityId()) {
                    double d = cur.distance(e.getPosition()); if (d <= radius) {
                        String name = "Unknown";
                        if (Bot.INSTANCE.players != null) { var p = Bot.INSTANCE.players.get(e.getUuid()); if (p != null) name = p.getName(); }
                        sb.append(String.format("  %s dist:%.1f ID:%d pos:(%.1f,%.1f,%.1f)\n", name, d, e.getEntityId(), e.getPosition().x, e.getPosition().y, e.getPosition().z)); count++;
                    }
                }
            }
            return count == 0 ? "No players within " + radius + " blocks." : "Found " + count + " players:\n" + sb;
        } catch (Exception e) { return "Error: " + e.getMessage(); }
    }
}
