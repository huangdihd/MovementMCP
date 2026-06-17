package movementmcp.tools.action;

import static movementmcp.McpHelpers.*;
import movementmcp.McpTool;
import com.google.gson.JsonObject;
import xin.bbtt.mcbot.Bot;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.*;
import java.util.List;

public class UseItemTool implements McpTool {
    public String name() { return "useItem"; }
    public String description() { return "Use held item (right-click once)."; }
    public List<Param> params() { return List.of(); }
    public String execute(JsonObject args) {
        Bot.INSTANCE.getSession().send(new ServerboundSwingPacket(Hand.MAIN_HAND));
        Bot.INSTANCE.getSession().send(new ServerboundUseItemPacket(Hand.MAIN_HAND, seq(), yaw(), pitch()));
        return "Used item.";
    }
}
