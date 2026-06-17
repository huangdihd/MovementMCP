package movementmcp;

import org.joml.Vector3d;
import xin.bbtt.Block.BlockState;
import xin.bbtt.Block.BlockStateParser;
import xin.bbtt.MovementSync;
import xin.bbtt.mcbot.Bot;

public class McpHelpers {

    public static boolean ready() { return MovementSync.INSTANCE != null && MovementSync.INSTANCE.position.get() != null; }
    public static Vector3d pos() { return MovementSync.INSTANCE.position.get(); }
    public static int seq() { return Bot.INSTANCE.getAndIncreaseSequence(); }
    public static float yaw() { return MovementSync.INSTANCE.yaw.get(); }
    public static float pitch() { return MovementSync.INSTANCE.pitch.get(); }

    public static BlockState stateAt(int x, int y, int z) {
        try { return BlockStateParser.Instance.parseStateId(MovementSync.INSTANCE.getWorld().getBlockAt(new Vector3d(x, y, z))); }
        catch (Exception e) { return null; }
    }

    public static String blockNameAt(int x, int y, int z) {
        BlockState s = stateAt(x, y, z); return s == null ? "Unknown" : s.blockName();
    }

    public static String relDesc(int dx, int dy, int dz) {
        StringBuilder sb = new StringBuilder();
        if (dx > 0) sb.append("E").append(dx); else if (dx < 0) sb.append("W").append(-dx);
        if (dz > 0) sb.append("S").append(dz); else if (dz < 0) sb.append("N").append(-dz);
        if (dy > 0) sb.append("U").append(dy); else if (dy < 0) sb.append("D").append(-dy);
        return sb.isEmpty() ? "here" : sb.toString();
    }
}
