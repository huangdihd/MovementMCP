package movementmcp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.joml.Vector3d;
import org.joml.Vector3i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xin.bbtt.Block.BlockState;
import xin.bbtt.Block.BlockStateParser;
import xin.bbtt.Entity.Entity;
import xin.bbtt.MovementSync;
import xin.bbtt.mcbot.Bot;
import xin.bbtt.movements.WalkMovement;
import xin.bbtt.movements.ActionMovement;
import xin.bbtt.movements.DigBlockMovement;
import xin.bbtt.pathfinding.DStarLite;
import xin.bbtt.pathfinding.Node;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerAction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.InteractAction;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.*;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

public class ToolBridge {

    private static final Logger logger = LoggerFactory.getLogger(ToolBridge.class);
    private final List<ToolDef> toolDefs = new ArrayList<>();
    private final Map<String, Function<JsonObject, String>> handlers = new HashMap<>();

    public ToolBridge() {
        initTools();
        logger.info("[MovementMCP] Registered {} tools", toolDefs.size());
    }

    // ==================== Tool Registration ====================

    private void reg(String name, String desc, Function<JsonObject, String> handler, Param... params) {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject props = new JsonObject();
        JsonArray req = new JsonArray();
        for (Param p : params) {
            JsonObject s = new JsonObject();
            s.addProperty("type", p.type);
            s.addProperty("description", p.desc);
            props.add(p.name, s);
            req.add(p.name);
        }
        schema.add("properties", props);
        schema.add("required", req);
        toolDefs.add(new ToolDef(name, desc, schema));
        handlers.put(name, handler);
    }

    private record Param(String name, String type, String desc) {
        Param(String name, String type) { this(name, type, ""); }
    }

    private static Param num(String name) { return new Param(name, "number"); }
    private static Param num(String name, String desc) { return new Param(name, "number", desc); }
    private static Param integer(String name) { return new Param(name, "integer"); }
    private static Param integer(String name, String desc) { return new Param(name, "integer", desc); }
    private static Param str(String name) { return new Param(name, "string"); }
    private static Param str(String name, String desc) { return new Param(name, "string", desc); }

    // ==================== Tool Definitions ====================

    private void initTools() {
        // --- Movement ---
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

        // --- Perception ---
        reg("whereAmI", "Get current position, server, facing direction.", args -> {
            if (!ready()) return "Plugin not ready.";
            Vector3d p = new Vector3d(pos());
            float yaw = MovementSync.INSTANCE.yaw.get() % 360;
            if (yaw < 0) yaw += 360;
            String facing = yaw < 45 || yaw >= 315 ? "South(+Z)" : yaw < 135 ? "West(-X)" : yaw < 225 ? "North(-Z)" : "East(+X)";
            return String.format("Server: %s, Pos: (%.2f, %.2f, %.2f), Facing: %s",
                    Bot.INSTANCE.getServer(), p.x, p.y, p.z, facing);
        });

        reg("scanSurroundings", "Scan nearby blocks, hazards, clearance.", args -> {
            if (!ready() || MovementSync.INSTANCE.getWorld() == null) return "World not available.";
            Vector3d p = pos();
            int bx = (int) Math.floor(p.x), by = (int) Math.floor(p.y), bz = (int) Math.floor(p.z);
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("At (%d, %d, %d).\n", bx, by, bz));
            sb.append("Below: ").append(blockNameAt(bx, by - 1, bz)).append("\n");
            BlockState feet = stateAt(bx, by, bz);
            if (feet != null && feet.isLiquid()) sb.append("WARNING: In ").append(feet.blockName()).append("!\n");
            BlockState head = stateAt(bx, by + 1, bz);
            if (head != null && head.isSolid()) sb.append("WARNING: Suffocating in ").append(head.blockName()).append("!\n");
            int clearance = 0;
            for (int dy = 2; dy <= 12; dy++) { if (stateAt(bx, by + dy, bz) != null && !stateAt(bx, by + dy, bz).isPassable()) break; clearance++; }
            sb.append("Headroom: ").append(clearance >= 11 ? "11+" : clearance).append(" blocks\n");
            int[][] dirs = {{0, -1}, {0, 1}, {1, 0}, {-1, 0}};
            String[] names = {"North(-Z)", "South(+Z)", "East(+X)", "West(-X)"};
            for (int i = 0; i < 4; i++) {
                String r = "Clear 6 blocks";
                for (int d = 1; d <= 6; d++) {
                    BlockState f = stateAt(bx + dirs[i][0] * d, by, bz + dirs[i][1] * d);
                    BlockState h = stateAt(bx + dirs[i][0] * d, by + 1, bz + dirs[i][1] * d);
                    BlockState block = f != null && !f.isPassable() ? f : h != null && !h.isPassable() ? h : null;
                    if (block != null) { r = d + " blocks: " + block.blockName(); break; }
                }
                sb.append(names[i]).append(": ").append(r).append("\n");
            }
            return sb.toString();
        });

        reg("getAreaMap", "Top-down character map of surroundings.", args -> {
            int radius = Math.max(2, Math.min(8, args.get("radius").getAsInt()));
            int yFrom = args.get("yFrom").getAsInt(), yTo = args.get("yTo").getAsInt();
            if (!ready() || MovementSync.INSTANCE.getWorld() == null) return "World not available.";
            Vector3d p = pos();
            int bx = (int) Math.floor(p.x), by = (int) Math.floor(p.y), bz = (int) Math.floor(p.z);
            if (yFrom > yTo) { int t = yFrom; yFrom = yTo; yTo = t; }
            yFrom = Math.max(-4, Math.min(4, yFrom)); yTo = Math.max(-4, Math.min(4, yTo));
            if (yTo - yFrom > 4) yTo = yFrom + 4;
            String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
            Map<String, Character> legend = new LinkedHashMap<>();
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Map @(%d,%d,%d) r%d. Up=N(-Z) Down=S(+Z) Left=W(-X) Right=E(+X).\n", bx, by, bz, radius));
            for (int dy = yTo; dy >= yFrom; dy--) {
                sb.append(String.format("── y=%d (%+d) ──\n", by + dy, dy));
                for (int dz = -radius; dz <= radius; dz++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        if (dx == 0 && dz == 0 && (dy == 0 || dy == 1)) { sb.append('@'); continue; }
                        BlockState s = stateAt(bx + dx, by + dy, bz + dz);
                        if (s == null) sb.append('?');
                        else if (s.isLiquid()) sb.append(s.blockName().contains("lava") ? '!' : '~');
                        else if (s.isPassable()) sb.append('.');
                        else {
                            Character sym = legend.computeIfAbsent(s.blockName(), k -> legend.size() < letters.length() ? letters.charAt(legend.size()) : '#');
                            sb.append(sym);
                        }
                    }
                    sb.append('\n');
                }
            }
            sb.append("Legend: @=you .=passable ~=water !=lava ?=unloaded\n");
            legend.forEach((k, v) -> { if (v != '#') sb.append(v).append("=").append(k).append(" "); });
            return sb.toString();
        }, integer("radius", "Horizontal radius (2-8)"), integer("yFrom", "Start Y offset from feet"), integer("yTo", "End Y offset from feet"));

        reg("getNearbyEntities", "List entities within radius.", args -> {
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
                for (Entity e : entities.values()) {
                    if (e != null && e.getPosition() != null && e.getEntityId() != MovementSync.INSTANCE.entityId && cur.distance(e.getPosition()) <= radius)
                        nearby.add(e);
                }
                if (nearby.isEmpty()) return "No entities within " + radius + " blocks.";
                nearby.sort(Comparator.comparingDouble(e -> cur.distance(e.getPosition())));
                StringBuilder sb = new StringBuilder(String.format("Found %d entities:\n", nearby.size()));
                for (int i = 0; i < Math.min(nearby.size(), 40); i++) {
                    Entity e = nearby.get(i);
                    sb.append(String.format("  [%s] ID:%d dist:%.1f\n", e.getType().name(), e.getEntityId(), cur.distance(e.getPosition())));
                }
                return sb.toString();
            } catch (Exception e) { return "Error: " + e.getMessage(); }
        }, num("radius", "Search radius in blocks"));

        reg("getBlocksInCube", "List non-air blocks in a cuboid region.", args -> {
            int x1 = args.get("x1").getAsInt(), y1 = args.get("y1").getAsInt(), z1 = args.get("z1").getAsInt();
            int x2 = args.get("x2").getAsInt(), y2 = args.get("y2").getAsInt(), z2 = args.get("z2").getAsInt();
            if (!ready() || MovementSync.INSTANCE.getWorld() == null) return "World not available.";
            int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
            int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
            int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
            if ((long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1) > 1000) return "Too many blocks (>1000).缩小范围.";
            Map<String, List<String>> groups = new HashMap<>();
            for (int x = minX; x <= maxX; x++) for (int y = minY; y <= maxY; y++) for (int z = minZ; z <= maxZ; z++) {
                int id = MovementSync.INSTANCE.getWorld().getBlockAt(new Vector3d(x, y, z));
                String name = String.valueOf(BlockStateParser.Instance.parseStateId(id));
                if (name.contains("air")) continue;
                groups.computeIfAbsent(name, k -> new ArrayList<>()).add(String.format("(%d,%d,%d)", x, y, z));
            }
            StringBuilder sb = new StringBuilder(String.format("Region (%d,%d,%d)-(%d,%d,%d):\n", minX, minY, minZ, maxX, maxY, maxZ));
            if (groups.isEmpty()) return sb.append("All air.").toString();
            groups.forEach((name, coords) -> {
                sb.append(String.format("  %s x%d: ", name, coords.size()));
                sb.append(coords.size() > 20 ? String.join(", ", coords.subList(0, 20)) + "...\n" : String.join(", ", coords) + "\n");
            });
            return sb.toString();
        }, integer("x1"), integer("y1"), integer("z1"), integer("x2"), integer("y2"), integer("z2"));

        reg("findSpecificBlocks", "Find blocks by name within radius.", args -> {
            String query = args.get("blockName").getAsString().toLowerCase();
            double radius = args.get("radius").getAsDouble();
            if (!ready() || MovementSync.INSTANCE.getWorld() == null) return "World not available.";
            Vector3d center = pos();
            int cx = (int) Math.floor(center.x), cy = (int) Math.floor(center.y), cz = (int) Math.floor(center.z);
            int r = (int) Math.ceil(radius);
            record Found(int x, int y, int z, double dist, String name) {}
            List<Found> found = new ArrayList<>();
            for (int x = cx - r; x <= cx + r; x++) for (int y = cy - r; y <= cy + r; y++) for (int z = cz - r; z <= cz + r; z++) {
                double d = center.distance(new Vector3d(x, y, z));
                if (d > radius) continue;
                BlockState s = stateAt(x, y, z);
                if (s != null && s.blockName().toLowerCase().contains(query) && !s.blockName().toLowerCase().contains("air"))
                    found.add(new Found(x, y, z, d, s.blockName()));
            }
            if (found.isEmpty()) return String.format("No '%s' found within %.1f blocks.", query, radius);
            found.sort(Comparator.comparingDouble(Found::dist));
            int shown = Math.min(found.size(), 30);
            StringBuilder sb = new StringBuilder(String.format("Found %d '%s' (showing %d):\n", found.size(), query, shown));
            for (int i = 0; i < shown; i++) {
                Found f = found.get(i);
                sb.append(String.format("  %s (%d,%d,%d) dist:%.1f [%s]\n", f.name(), f.x(), f.y(), f.z(), f.dist(), relDesc(f.x() - cx, f.y() - cy, f.z() - cz)));
            }
            return sb.toString();
        }, str("blockName", "Block name to search (partial match)"), num("radius", "Search radius"));

        // --- Action ---
        reg("interactEntity", "Interact with entity (ATTACK or INTERACT).", args -> {
            int entityId = args.get("entityId").getAsInt();
            InteractAction action;
            try { action = InteractAction.valueOf(args.get("action").getAsString().toUpperCase()); }
            catch (Exception e) { return "Invalid action. Use ATTACK or INTERACT."; }
            if (ready() && MovementSync.INSTANCE.getWorld() != null) {
                Entity ent = MovementSync.INSTANCE.getWorld().getEntity(entityId);
                if (ent != null && ent.getPosition() != null) MovementSync.INSTANCE.lookAt(ent.getPosition());
            }
            Bot.INSTANCE.getSession().send(new ServerboundSwingPacket(Hand.MAIN_HAND));
            Bot.INSTANCE.getSession().send(action == InteractAction.ATTACK
                    ? new ServerboundInteractPacket(entityId, action, false)
                    : new ServerboundInteractPacket(entityId, action, Hand.MAIN_HAND, false));
            return "Interacted entity " + entityId + " (" + action + ")";
        }, integer("entityId", "Entity ID"), str("action", "ATTACK or INTERACT"));

        reg("changeSlot", "Switch hotbar slot (0-8).", args -> {
            int slot = args.get("slot").getAsInt();
            if (slot < 0 || slot > 8) return "Slot must be 0-8.";
            Bot.INSTANCE.getSession().send(new ServerboundSetCarriedItemPacket(slot));
            return "Switched to slot " + slot;
        }, integer("slot", "Hotbar slot 0-8"));

        reg("useItem", "Use held item (right-click once).", args -> {
            Bot.INSTANCE.getSession().send(new ServerboundSwingPacket(Hand.MAIN_HAND));
            Bot.INSTANCE.getSession().send(new ServerboundUseItemPacket(Hand.MAIN_HAND, seq(), yaw(), pitch()));
            return "Used item.";
        });

        reg("useItemWithDuration", "Hold use item for duration (e.g. eat 1600ms, bow 1000ms).", args -> {
            long ms = args.get("durationMs").getAsLong();
            Bot.INSTANCE.getSession().send(new ServerboundSwingPacket(Hand.MAIN_HAND));
            Bot.INSTANCE.getSession().send(new ServerboundUseItemPacket(Hand.MAIN_HAND, seq(), yaw(), pitch()));
            new Thread(() -> {
                try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
                if (Bot.INSTANCE.getSession() != null)
                    Bot.INSTANCE.getSession().send(new ServerboundPlayerActionPacket(PlayerAction.RELEASE_USE_ITEM, org.cloudburstmc.math.vector.Vector3i.from(0, 0, 0), Direction.DOWN, seq()));
            }, "mcp-useitem-release").start();
            return "Using item for " + ms + "ms.";
        }, integer("durationMs", "Hold duration in milliseconds"));

        reg("releaseUseItem", "Release use item key.", args -> {
            Bot.INSTANCE.getSession().send(new ServerboundPlayerActionPacket(PlayerAction.RELEASE_USE_ITEM, org.cloudburstmc.math.vector.Vector3i.from(0, 0, 0), Direction.DOWN, seq()));
            return "Released item.";
        });

        reg("interactBlock", "Interact with block (place, press button, open chest, etc).", args -> {
            int x = args.get("x").getAsInt(), y = args.get("y").getAsInt(), z = args.get("z").getAsInt();
            Direction dir;
            try { dir = Direction.valueOf(args.get("direction").getAsString().toUpperCase()); }
            catch (Exception e) { return "Invalid direction. Use DOWN/UP/NORTH/SOUTH/WEST/EAST."; }
            Vector3d cur = pos();
            if (cur.distance(new Vector3d(x + 0.5, y + 0.5, z + 0.5)) > 6) return "Too far (max 6 blocks).";
            MovementSync.INSTANCE.lookAt(new Vector3d(x + 0.5, y + 0.5, z + 0.5));
            Bot.INSTANCE.getSession().send(new ServerboundSwingPacket(Hand.MAIN_HAND));
            Bot.INSTANCE.getSession().send(new ServerboundUseItemOnPacket(org.cloudburstmc.math.vector.Vector3i.from(x, y, z), dir, Hand.MAIN_HAND, 0.5f, 0.5f, 0.5f, false, false, seq()));
            return String.format("Interacted block (%d,%d,%d).", x, y, z);
        }, integer("x"), integer("y"), integer("z"), str("direction", "Face: DOWN/UP/NORTH/SOUTH/WEST/EAST"));

        reg("mineBlock", "Mine block at coordinates.", args -> {
            int x = args.get("x").getAsInt(), y = args.get("y").getAsInt(), z = args.get("z").getAsInt();
            MovementSync.INSTANCE.lookAt(new Vector3d(x + 0.5, y + 0.5, z + 0.5));
            MovementSync.INSTANCE.getMovementController().addMovement(new DigBlockMovement(org.cloudburstmc.math.vector.Vector3i.from(x, y, z)));
            return "Mining (" + x + "," + y + "," + z + ")";
        }, integer("x"), integer("y"), integer("z"));

        // --- Social ---
        reg("getPlayerList", "List online players.", args -> {
            if (Bot.INSTANCE == null || Bot.INSTANCE.players == null || Bot.INSTANCE.players.isEmpty()) return "No player info.";
            List<String> names = Bot.INSTANCE.players.values().stream().map(org.geysermc.mcprotocollib.auth.GameProfile::getName).toList();
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
                    if (e.getType() == org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType.PLAYER && MovementSync.INSTANCE.entityId != e.getEntityId()) {
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

        // --- System ---
        reg("sendChatMessage", "Send chat message visible to all.", args -> {
            String msg = args.get("message").getAsString();
            if (Bot.INSTANCE == null) return "Bot not ready.";
            sendChatInChunks(msg);
            return "Message sent.";
        }, str("message", "Message to send"));

        reg("sendCommand", "Execute server command (without leading /).", args -> {
            String cmd = args.get("command").getAsString();
            if (Bot.INSTANCE == null) return "Bot not ready.";
            Bot.INSTANCE.sendCommand(cmd);
            return "Command sent: " + cmd;
        }, str("command", "Command text without /"));
    }

    // ==================== MCP Interface ====================

    public JsonArray listTools() {
        JsonArray arr = new JsonArray();
        for (ToolDef td : toolDefs) {
            JsonObject t = new JsonObject();
            t.addProperty("name", td.name);
            t.addProperty("description", td.desc);
            t.add("inputSchema", td.schema);
            arr.add(t);
        }
        return arr;
    }

    public String callTool(String name, JsonObject args) throws Exception {
        Function<JsonObject, String> handler = handlers.get(name);
        if (handler == null) throw new IllegalArgumentException("Unknown tool: " + name);
        return handler.apply(args);
    }

    // ==================== Helpers ====================

    private boolean ready() { return MovementSync.INSTANCE != null && MovementSync.INSTANCE.position.get() != null; }
    private Vector3d pos() { return MovementSync.INSTANCE.position.get(); }
    private int seq() { return Bot.INSTANCE.getAndIncreaseSequence(); }
    private float yaw() { return MovementSync.INSTANCE.yaw.get(); }
    private float pitch() { return MovementSync.INSTANCE.pitch.get(); }

    private BlockState stateAt(int x, int y, int z) {
        try { return BlockStateParser.Instance.parseStateId(MovementSync.INSTANCE.getWorld().getBlockAt(new Vector3d(x, y, z))); }
        catch (Exception e) { return null; }
    }

    private String blockNameAt(int x, int y, int z) { BlockState s = stateAt(x, y, z); return s == null ? "Unknown" : s.blockName(); }

    private static String relDesc(int dx, int dy, int dz) {
        StringBuilder sb = new StringBuilder();
        if (dx > 0) sb.append("E").append(dx); else if (dx < 0) sb.append("W").append(-dx);
        if (dz > 0) sb.append("S").append(dz); else if (dz < 0) sb.append("N").append(-dz);
        if (dy > 0) sb.append("U").append(dy); else if (dy < 0) sb.append("D").append(-dy);
        return sb.isEmpty() ? "here" : sb.toString();
    }

    private void sendChatInChunks(String text) {
        int limit = 90;
        StringBuilder chunk = new StringBuilder();
        int bytes = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int cb = String.valueOf(c).getBytes(StandardCharsets.UTF_8).length;
            if (bytes + cb > limit) { Bot.INSTANCE.sendChatMessage(chunk.toString()); chunk = new StringBuilder(); bytes = 0; try { Thread.sleep(250); } catch (InterruptedException ignored) {} }
            chunk.append(c); bytes += cb;
        }
        if (chunk.length() > 0) Bot.INSTANCE.sendChatMessage(chunk.toString());
    }

    private record ToolDef(String name, String desc, JsonObject schema) {}
}
