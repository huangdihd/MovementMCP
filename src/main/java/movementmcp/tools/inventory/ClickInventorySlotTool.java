package movementmcp.tools.inventory;

import static movementmcp.McpHelpers.*;
import movementmcp.McpTool;
import com.google.gson.JsonObject;
import xin.bbtt.MovementSync;
import xin.bbtt.mcbot.Bot;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ClickItemAction;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerActionType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.List;

public class ClickInventorySlotTool implements McpTool {
    public String name() { return "clickInventorySlot"; }
    public String description() { return "Click an inventory slot to move/equip/drop items."; }
    public List<Param> params() { return List.of(Param.integer("slot", "Slot number (0-45, hotbar 36-44)"), Param.integer("button", "0=left click, 1=right click")); }
    public String execute(JsonObject args) {
        int slot = args.get("slot").getAsInt();
        int button = args.get("button").getAsInt();
        if (!ready()) return "MovementSync not ready.";
        var im = MovementSync.INSTANCE.inventoryManager;
        int containerId = im.getCurrentContainerId();
        int stateId = im.getCurrentStateId();
        ClickItemAction action = button == 1 ? ClickItemAction.RIGHT_CLICK : ClickItemAction.LEFT_CLICK;
        Bot.INSTANCE.getSession().send(new ServerboundContainerClickPacket(
                containerId, stateId, slot, ContainerActionType.CLICK_ITEM, action, null, new Int2ObjectOpenHashMap<>()));
        return "Clicked slot " + slot + " (" + action + ").";
    }
}
