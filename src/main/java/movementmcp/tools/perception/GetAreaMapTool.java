package movementmcp.tools.perception;

import static movementmcp.McpHelpers.*;
import movementmcp.McpTool;
import com.google.gson.JsonObject;
import org.joml.Vector3d;
import xin.bbtt.MovementSync;
import xin.bbtt.Block.BlockState;
import java.util.*;

public class GetAreaMapTool implements McpTool {
    public String name() { return "getAreaMap"; }
    public String description() { return "Top-down character map of surroundings."; }
    public List<Param> params() { return List.of(Param.integer("radius", "Horizontal radius (2-8)"), Param.integer("yFrom", "Start Y offset from feet"), Param.integer("yTo", "End Y offset from feet")); }
    public String execute(JsonObject args) {
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
                    if (s == null) sb.append('?'); else if (s.isLiquid()) sb.append(s.blockName().contains("lava") ? '!' : '~');
                    else if (s.isPassable()) sb.append('.');
                    else { Character sym = legend.get(s.blockName()); if (sym == null) { sym = legend.size() < letters.length() ? letters.charAt(legend.size()) : '#'; legend.put(s.blockName(), sym); } sb.append(sym); }
                }
                sb.append('\n');
            }
        }
        sb.append("Legend: @=you .=passable ~=water !=lava ?=unloaded\n");
        legend.forEach((k, v) -> { if (v != '#') sb.append(v).append("=").append(k).append(" "); });
        return sb.toString();
    }
}
