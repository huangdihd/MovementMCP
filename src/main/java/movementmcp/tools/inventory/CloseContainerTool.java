package movementmcp.tools.inventory;

import static movementmcp.McpHelpers.*;
import movementmcp.McpTool;
import com.google.gson.JsonObject;
import xin.bbtt.MovementSync;
import xin.bbtt.mcbot.Bot;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClosePacket;
import java.util.List;

public class CloseContainerTool implements McpTool {
    public String name() { return "closeContainer"; }
    public String description() { return "Close currently opened container."; }
    public List<Param> params() { return List.of(); }
    public String execute(JsonObject args) {
        if (!ready()) return "MovementSync not ready.";
        int containerId = MovementSync.INSTANCE.inventoryManager.getCurrentContainerId();
        if (containerId == 0) return "No external container open.";
        Bot.INSTANCE.getSession().send(new ServerboundContainerClosePacket(containerId));
        return "Close container request sent.";
    }
}
