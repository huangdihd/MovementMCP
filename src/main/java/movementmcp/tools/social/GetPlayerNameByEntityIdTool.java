package movementmcp.tools.social;

import static movementmcp.McpHelpers.*;
import movementmcp.McpTool;
import com.google.gson.JsonObject;
import xin.bbtt.MovementSync;
import xin.bbtt.mcbot.Bot;
import xin.bbtt.Entity.Entity;
import java.util.List;

public class GetPlayerNameByEntityIdTool implements McpTool {
    public String name() { return "getPlayerNameByEntityId"; }
    public String description() { return "Get player name by entity ID."; }
    public List<Param> params() { return List.of(Param.integer("entityId", "Entity ID")); }
    public String execute(JsonObject args) {
        int id = args.get("entityId").getAsInt();
        if (MovementSync.INSTANCE == null || MovementSync.INSTANCE.getWorld() == null) return "World not loaded.";
        Entity ent = MovementSync.INSTANCE.getWorld().getEntity(id);
        if (ent == null) return "Entity " + id + " not found (may have left render distance).";
        var uuid = ent.getUuid();
        if (Bot.INSTANCE.players != null) {
            var profile = Bot.INSTANCE.players.get(uuid);
            if (profile != null) return "Entity " + id + " is player: " + profile.getName();
        }
        return "Entity " + id + " is type " + ent.getType().name() + ", no associated player name.";
    }
}
