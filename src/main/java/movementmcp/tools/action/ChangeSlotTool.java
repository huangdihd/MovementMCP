package movementmcp.tools.action;

import static movementmcp.McpHelpers.*;
import movementmcp.McpTool;
import com.google.gson.JsonObject;
import xin.bbtt.mcbot.Bot;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundSetCarriedItemPacket;
import java.util.List;

public class ChangeSlotTool implements McpTool {
    public String name() { return "changeSlot"; }
    public String description() { return "Switch hotbar slot (0-8)."; }
    public List<Param> params() { return List.of(Param.integer("slot", "Hotbar slot 0-8")); }
    public String execute(JsonObject args) {
        int slot = args.get("slot").getAsInt();
        if (slot < 0 || slot > 8) return "Slot must be 0-8.";
        Bot.INSTANCE.getSession().send(new ServerboundSetCarriedItemPacket(slot));
        return "Switched to slot " + slot;
    }
}
