package movementmcp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.joml.Vector3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xin.bbtt.Block.BlockState;
import xin.bbtt.Block.BlockStateParser;
import xin.bbtt.MovementSync;
import xin.bbtt.mcbot.Bot;

import java.util.*;
import java.util.function.Function;

public abstract class BaseMcpTools {

    protected static final Logger logger = LoggerFactory.getLogger(BaseMcpTools.class);

    final List<ToolDef> defs = new ArrayList<>();
    final Map<String, Function<JsonObject, String>> handlers = new HashMap<>();

    protected abstract void init();

    // ==================== Registration ====================

    protected void reg(String name, String desc, Function<JsonObject, String> handler, Param... params) {
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
        defs.add(new ToolDef(name, desc, schema));
        handlers.put(name, handler);
    }

    protected record Param(String name, String type, String desc) {
        Param(String name, String type) { this(name, type, ""); }
    }

    protected static Param num(String name) { return new Param(name, "number"); }
    protected static Param num(String name, String desc) { return new Param(name, "number", desc); }
    protected static Param integer(String name) { return new Param(name, "integer"); }
    protected static Param integer(String name, String desc) { return new Param(name, "integer", desc); }
    protected static Param str(String name) { return new Param(name, "string"); }
    protected static Param str(String name, String desc) { return new Param(name, "string", desc); }

    // ==================== Helpers ====================

    protected static boolean ready() { return MovementSync.INSTANCE != null && MovementSync.INSTANCE.position.get() != null; }
    protected static Vector3d pos() { return MovementSync.INSTANCE.position.get(); }
    protected static int seq() { return Bot.INSTANCE.getAndIncreaseSequence(); }
    protected static float yaw() { return MovementSync.INSTANCE.yaw.get(); }
    protected static float pitch() { return MovementSync.INSTANCE.pitch.get(); }

    protected static BlockState stateAt(int x, int y, int z) {
        try { return BlockStateParser.Instance.parseStateId(MovementSync.INSTANCE.getWorld().getBlockAt(new Vector3d(x, y, z))); }
        catch (Exception e) { return null; }
    }

    protected static String blockNameAt(int x, int y, int z) {
        BlockState s = stateAt(x, y, z); return s == null ? "Unknown" : s.blockName();
    }

    protected static String relDesc(int dx, int dy, int dz) {
        StringBuilder sb = new StringBuilder();
        if (dx > 0) sb.append("E").append(dx); else if (dx < 0) sb.append("W").append(-dx);
        if (dz > 0) sb.append("S").append(dz); else if (dz < 0) sb.append("N").append(-dz);
        if (dy > 0) sb.append("U").append(dy); else if (dy < 0) sb.append("D").append(-dy);
        return sb.isEmpty() ? "here" : sb.toString();
    }

    record ToolDef(String name, String desc, JsonObject schema) {}
}
