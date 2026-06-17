package movementmcp.tools.perception;

import static movementmcp.McpHelpers.*;
import movementmcp.McpTool;
import com.google.gson.JsonObject;
import org.joml.Vector3d;
import xin.bbtt.MovementSync;
import xin.bbtt.Block.BlockStateParser;
import java.util.*;

public class GetBlocksInCubeTool implements McpTool {
    public String name() { return "getBlocksInCube"; }
    public String description() { return "List non-air blocks in a cuboid region."; }
    public List<Param> params() { return List.of(Param.integer("x1"), Param.integer("y1"), Param.integer("z1"), Param.integer("x2"), Param.integer("y2"), Param.integer("z2")); }
    public String execute(JsonObject args) {
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
        groups.forEach((name, coords) -> { sb.append(String.format("  %s x%d: ", name, coords.size())); sb.append(coords.size() > 20 ? String.join(", ", coords.subList(0, 20)) + "...\n" : String.join(", ", coords) + "\n"); });
        return sb.toString();
    }
}
