package movementmcp.tools.inventory;

import static movementmcp.McpHelpers.*;
import movementmcp.McpTool;
import movementmcp.utils.ItemNameResolver;
import com.google.gson.JsonObject;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import xin.bbtt.MovementSync;
import java.util.List;

public class GetInventoryTool implements McpTool {
    public String name() { return "getInventory"; }
    public String description() { return "Get bot inventory or currently opened container."; }
    public List<Param> params() { return List.of(); }
    public String execute(JsonObject args) {
        if (!ready()) return "MovementSync not ready.";
        var im = MovementSync.INSTANCE.inventoryManager;
        int containerId = im.getCurrentContainerId();
        ItemStack[] items = im.getInventory();
        if (items == null) return "No inventory data yet.";
        StringBuilder sb = new StringBuilder();
        sb.append(containerId == 0 ? "Inventory:\n" : "Container (ID: " + containerId + "):\n");
        int count = 0;
        for (int i = 0; i < items.length; i++) {
            ItemStack item = items[i];
            if (item != null) {
                String itemName = ItemNameResolver.getItemName(item.getId());
                sb.append(String.format("  Slot %d: %s x%d\n", i, itemName, item.getAmount()));
                count++;
            }
        }
        return count == 0 ? "Empty." : sb.toString();
    }
}
