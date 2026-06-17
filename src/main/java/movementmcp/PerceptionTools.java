package movementmcp;

import org.joml.Vector3d;
import xin.bbtt.MovementSync;
import xin.bbtt.mcbot.Bot;
import xin.bbtt.Block.BlockState;
import xin.bbtt.Block.BlockStateParser;

import java.util.*;

public class PerceptionTools extends BaseMcpTools {

    @Override
    protected void init() {
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
            for (int dy = 2; dy <= 12; dy++) {
                BlockState s = stateAt(bx, by + dy, bz);
                if (s == null || !s.isPassable()) break;
                clearance++;
            }
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

            String[] hazards = {"lava", "fire", "magma", "cactus", "sweet_berry", "powder_snow"};
            List<String> found = new ArrayList<>();
            outer:
            for (int dx = -4; dx <= 4; dx++) for (int dy = -2; dy <= 3; dy++) for (int dz = -4; dz <= 4; dz++) {
                BlockState s = stateAt(bx + dx, by + dy, bz + dz);
                if (s == null) continue;
                String n = s.blockName().toLowerCase();
                for (String hz : hazards) {
                    if (n.contains(hz)) {
                        found.add(s.blockName() + "(" + relDesc(dx, dy, dz) + ")");
                        if (found.size() >= 8) break outer;
                        break;
                    }
                }
            }
            sb.append(found.isEmpty() ? "No hazards in 4 block radius.\n" : "Hazards: " + String.join(", ", found) + "\n");
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
                            Character sym = legend.size() < letters.length() ? letters.charAt(legend.size()) : '#';
                            legend.putIfAbsent(s.blockName(), sym);
                            sb.append(legend.get(s.blockName()));
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
                Map<Integer, xin.bbtt.Entity.Entity> entities = (Map<Integer, xin.bbtt.Entity.Entity>) field.get(MovementSync.INSTANCE.getWorld());
                if (entities == null || entities.isEmpty()) return "No entities cached.";
                List<xin.bbtt.Entity.Entity> nearby = new ArrayList<>();
                for (xin.bbtt.Entity.Entity e : entities.values()) {
                    if (e != null && e.getPosition() != null && e.getEntityId() != MovementSync.INSTANCE.entityId && cur.distance(e.getPosition()) <= radius)
                        nearby.add(e);
                }
                if (nearby.isEmpty()) return "No entities within " + radius + " blocks.";
                nearby.sort(Comparator.comparingDouble(e -> cur.distance(e.getPosition())));
                StringBuilder sb = new StringBuilder(String.format("Found %d entities:\n", nearby.size()));
                for (int i = 0; i < Math.min(nearby.size(), 40); i++) {
                    xin.bbtt.Entity.Entity e = nearby.get(i);
                    sb.append(String.format("  [%s] ID:%d dist:%.1f\n", e.getType().name(), e.getEntityId(), cur.distance(e.getPosition())));
                }
                return sb.toString();
            } catch (Exception e) { return "Error: " + e.getMessage(); }
        }, num("radius", "Search radius in blocks"));

        reg("getBlocksInCube", "List non-air blocks in a cuboid region.", args -> {
            int x1 = args.get("x1").getAsInt(), y1 = args.get("y1").getAsInt(), z1 = args.get("z1").getAsInt();
            int x2 = args.get("x2").getAsInt(), y2 = args.get("y2").getAsInt(), z2 = args.get("z2").getAsInt();
            if (!ready() || MovementSync.INSTANCE.getWorld() == null) return "World not available.";
            int minX = Math.min(x1, x2), maxX = Math.max(x1, x2), minY = Math.min(y1, y2), maxY = Math.max(y1, y2), minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
            if ((long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1) > 1000) return "Too many blocks (>1000).";
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
    }
}
