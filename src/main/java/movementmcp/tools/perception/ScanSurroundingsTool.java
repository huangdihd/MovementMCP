package movementmcp.tools.perception;

import static movementmcp.McpHelpers.*;
import movementmcp.McpTool;
import com.google.gson.JsonObject;
import org.joml.Vector3d;
import xin.bbtt.MovementSync;
import xin.bbtt.Block.BlockState;
import java.util.*;

public class ScanSurroundingsTool implements McpTool {
    public String name() { return "scanSurroundings"; }
    public String description() { return "Scan nearby blocks, hazards, clearance."; }
    public List<Param> params() { return List.of(); }
    public String execute(JsonObject args) {
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
        for (int dy = 2; dy <= 12; dy++) { BlockState s = stateAt(bx, by + dy, bz); if (s == null || !s.isPassable()) break; clearance++; }
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
        outer: for (int dx = -4; dx <= 4; dx++) for (int dy = -2; dy <= 3; dy++) for (int dz = -4; dz <= 4; dz++) {
            BlockState s = stateAt(bx + dx, by + dy, bz + dz); if (s == null) continue;
            for (String hz : hazards) { if (s.blockName().toLowerCase().contains(hz)) { found.add(s.blockName() + "(" + relDesc(dx, dy, dz) + ")"); if (found.size() >= 8) break outer; break; } }
        }
        sb.append(found.isEmpty() ? "No hazards in 4 block radius.\n" : "Hazards: " + String.join(", ", found) + "\n");
        return sb.toString();
    }
}
