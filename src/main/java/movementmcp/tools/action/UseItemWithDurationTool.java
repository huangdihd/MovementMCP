package movementmcp.tools.action;

import static movementmcp.McpHelpers.*;
import movementmcp.McpTool;
import com.google.gson.JsonObject;
import xin.bbtt.mcbot.Bot;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerAction;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.*;
import java.util.List;

public class UseItemWithDurationTool implements McpTool {
    public String name() { return "useItemWithDuration"; }
    public String description() { return "Hold use item for duration (e.g. eat 1600ms, bow 1000ms)."; }
    public List<Param> params() { return List.of(Param.integer("durationMs", "Hold duration in milliseconds")); }
    public String execute(JsonObject args) {
        long ms = args.get("durationMs").getAsLong();
        Bot.INSTANCE.getSession().send(new ServerboundSwingPacket(Hand.MAIN_HAND));
        Bot.INSTANCE.getSession().send(new ServerboundUseItemPacket(Hand.MAIN_HAND, seq(), yaw(), pitch()));
        new Thread(() -> {
            try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
            if (Bot.INSTANCE.getSession() != null)
                Bot.INSTANCE.getSession().send(new ServerboundPlayerActionPacket(
                        PlayerAction.RELEASE_USE_ITEM, org.cloudburstmc.math.vector.Vector3i.from(0, 0, 0), Direction.DOWN, seq()));
        }, "mcp-useitem-release").start();
        return "Using item for " + ms + "ms.";
    }
}
