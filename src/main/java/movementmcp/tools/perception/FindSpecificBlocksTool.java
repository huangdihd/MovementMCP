package movementmcp.tools.perception;

import static movementmcp.McpHelpers.*;
import movementmcp.McpTool;
import com.google.gson.JsonObject;
import org.joml.Vector3d;
import xin.bbtt.MovementSync;
import xin.bbtt.Block.BlockState;
import java.util.*;

public class FindSpecificBlocksTool implements McpTool {
    public String name() { return "findSpecificBlocks"; }
    public String description() { return "Find blocks by name within radius."; }
    public List<Param> params() { return List.of(Param.str("blockName", "Block name to search (partial match)"), Param.num("radius", "Search radius")); }
    public String execute(JsonObject args) {
        String query = args.get("blockName").getAsString().toLowerCase();
        double radius = args.get("radius").getAsDouble();
        if (!ready() || MovementSync.INSTANCE.getWorld() == null) return "World not available.";
        Vector3d center = pos();
        int cx = (int) Math.floor(center.x), cy = (int) Math.floor(center.y), cz = (int) Math.floor(center.z);
        int r = (int) Math.ceil(radius);
        record Found(int x, int y, int z, double dist, String name) {}
        List<Found> found = new ArrayList<>();
        for (int x = cx - r; x <= cx + r; x++) for (int y = cy - r; y <= cy + r; y++) for (int z = cz - r; z <= cz + r; z++) {
            double d = center.distance(new Vector3d(x, y, z)); if (d > radius) continue;
            BlockState s = stateAt(x, y, z);
            if (s != null && s.blockName().toLowerCase().contains(query) && !s.blockName().toLowerCase().contains("air"))
                found.add(new Found(x, y, z, d, s.blockName()));
        }
        if (found.isEmpty()) return String.format("No '%s' found within %.1f blocks.", query, radius);
        found.sort(Comparator.comparingDouble(Found::dist));
        int shown = Math.min(found.size(), 30);
        StringBuilder sb = new StringBuilder(String.format("Found %d '%s' (showing %d):\n", found.size(), query, shown));
        for (int i = 0; i < shown; i++) { Found f = found.get(i); sb.append(String.format("  %s (%d,%d,%d) dist:%.1f [%s]\n", f.name(), f.x(), f.y(), f.z(), f.dist(), relDesc(f.x() - cx, f.y() - cy, f.z() - cz))); }
        return sb.toString();
    }
}
