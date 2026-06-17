package movementmcp;

import org.joml.Vector3d;
import xin.bbtt.MovementSync;
import xin.bbtt.mcbot.Bot;
import xin.bbtt.Entity.Entity;

import java.util.Map;

public class SocialTools extends BaseMcpTools {

    @Override
    protected void init() {
        reg("getPlayerList", "List online players.", args -> {
            if (Bot.INSTANCE == null || Bot.INSTANCE.players == null || Bot.INSTANCE.players.isEmpty())
                return "No player info.";
            var names = Bot.INSTANCE.players.values().stream()
                    .map(org.geysermc.mcprotocollib.auth.GameProfile::getName).toList();
            return "Online (" + names.size() + "): " + String.join(", ", names);
        });

        reg("getNearbyPlayers", "List nearby players with details.", args -> {
            double radius = args.get("radius").getAsDouble();
            if (!ready() || MovementSync.INSTANCE.getWorld() == null) return "World not available.";
            Vector3d cur = pos();
            try {
                var field = xin.bbtt.world.World.class.getDeclaredField("entities");
                field.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<Integer, Entity> entities = (Map<Integer, Entity>) field.get(MovementSync.INSTANCE.getWorld());
                if (entities == null) return "No entities.";
                StringBuilder sb = new StringBuilder();
                int count = 0;
                for (Entity e : entities.values()) {
                    if (e == null || e.getPosition() == null) continue;
                    if (e.getType() == org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType.PLAYER
                            && MovementSync.INSTANCE.entityId != e.getEntityId()) {
                        double d = cur.distance(e.getPosition());
                        if (d <= radius) {
                            String name = "Unknown";
                            if (Bot.INSTANCE.players != null) {
                                var p = Bot.INSTANCE.players.get(e.getUuid());
                                if (p != null) name = p.getName();
                            }
                            sb.append(String.format("  %s dist:%.1f ID:%d\n", name, d, e.getEntityId()));
                            count++;
                        }
                    }
                }
                return count == 0 ? "No players within " + radius + " blocks." : "Found " + count + " players:\n" + sb;
            } catch (Exception e) { return "Error: " + e.getMessage(); }
        }, num("radius", "Search radius"));

        reg("followPlayer", "Follow a player by entity ID.", args -> {
            int id = args.get("entityId").getAsInt();
            if (!ready()) return "MovementSync not ready.";
            Entity ent = MovementSync.INSTANCE.getWorld().getEntity(id);
            if (ent == null) return "Entity " + id + " not found.";
            MovementSync.INSTANCE.setFollowTargetId(id);
            MovementSync.INSTANCE.triggerAutoRepath();
            return "Following entity " + id;
        }, integer("entityId", "Entity ID to follow"));

        reg("stopFollowing", "Stop following.", args -> {
            if (!ready()) return "MovementSync not ready.";
            MovementSync.INSTANCE.setFollowTargetId(-1);
            MovementSync.INSTANCE.getMovementController().cancelAll();
            return "Stopped following.";
        });
    }
}
