package movementmcp.tools.action;

import static movementmcp.McpHelpers.*;
import movementmcp.McpTool;
import com.google.gson.JsonObject;
import org.joml.Vector3d;
import xin.bbtt.MovementSync;
import xin.bbtt.mcbot.Bot;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.*;
import java.util.List;

public class InteractBlockTool implements McpTool {
    public String name() { return "interactBlock"; }
    public String description() { return "Interact with block (place, press button, open chest, etc)."; }
    public List<Param> params() { return List.of(Param.integer("x"), Param.integer("y"), Param.integer("z"), Param.str("direction", "Face: DOWN/UP/NORTH/SOUTH/WEST/EAST")); }
    public String execute(JsonObject args) {
        int x = args.get("x").getAsInt(), y = args.get("y").getAsInt(), z = args.get("z").getAsInt();
        Direction dir;
        try { dir = Direction.valueOf(args.get("direction").getAsString().toUpperCase()); }
        catch (Exception e) { return "Invalid direction. Use DOWN/UP/NORTH/SOUTH/WEST/EAST."; }
        Vector3d cur = pos();
        if (cur.distance(new Vector3d(x + 0.5, y + 0.5, z + 0.5)) > 6) return "Too far (max 6 blocks).";
        MovementSync.INSTANCE.lookAt(new Vector3d(x + 0.5, y + 0.5, z + 0.5));
        Bot.INSTANCE.getSession().send(new ServerboundSwingPacket(Hand.MAIN_HAND));
        Bot.INSTANCE.getSession().send(new ServerboundUseItemOnPacket(
                org.cloudburstmc.math.vector.Vector3i.from(x, y, z), dir, Hand.MAIN_HAND, 0.5f, 0.5f, 0.5f, false, false, seq()));
        return String.format("Interacted block (%d,%d,%d).", x, y, z);
    }
}
