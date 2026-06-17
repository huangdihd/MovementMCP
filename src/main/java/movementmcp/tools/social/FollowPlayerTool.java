package movementmcp.tools.social;

import static movementmcp.McpHelpers.*;
import movementmcp.McpTool;
import com.google.gson.JsonObject;
import xin.bbtt.MovementSync;
import xin.bbtt.Entity.Entity;
import java.util.List;

public class FollowPlayerTool implements McpTool {
    public String name() { return "followPlayer"; }
    public String description() { return "Follow a player by entity ID."; }
    public List<Param> params() { return List.of(Param.integer("entityId", "Entity ID to follow")); }
    public String execute(JsonObject args) {
        int id = args.get("entityId").getAsInt();
        if (!ready()) return "MovementSync not ready.";
        Entity ent = MovementSync.INSTANCE.getWorld().getEntity(id);
        if (ent == null) return "Entity " + id + " not found.";
        MovementSync.INSTANCE.setFollowTargetId(id);
        MovementSync.INSTANCE.triggerAutoRepath();
        return "Following entity " + id;
    }
}
