package movementmcp.tools.action;

import static movementmcp.McpHelpers.*;
import movementmcp.McpTool;
import com.google.gson.JsonObject;
import xin.bbtt.mcbot.Bot;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerAction;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket;
import java.util.List;

public class ReleaseUseItemTool implements McpTool {
    public String name() { return "releaseUseItem"; }
    public String description() { return "Release use item key."; }
    public List<Param> params() { return List.of(); }
    public String execute(JsonObject args) {
        Bot.INSTANCE.getSession().send(new ServerboundPlayerActionPacket(
                PlayerAction.RELEASE_USE_ITEM, org.cloudburstmc.math.vector.Vector3i.from(0, 0, 0), Direction.DOWN, seq()));
        return "Released item.";
    }
}
