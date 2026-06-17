package movementmcp.tools.action;

import static movementmcp.McpHelpers.*;
import movementmcp.McpTool;
import com.google.gson.JsonObject;
import xin.bbtt.MovementSync;
import xin.bbtt.mcbot.Bot;
import xin.bbtt.Entity.Entity;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.InteractAction;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.*;
import java.util.List;

public class InteractEntityTool implements McpTool {
    public String name() { return "interactEntity"; }
    public String description() { return "Interact with entity (ATTACK or INTERACT)."; }
    public List<Param> params() { return List.of(Param.integer("entityId", "Entity ID"), Param.str("action", "ATTACK or INTERACT")); }
    public String execute(JsonObject args) {
        int entityId = args.get("entityId").getAsInt();
        InteractAction action;
        try { action = InteractAction.valueOf(args.get("action").getAsString().toUpperCase()); }
        catch (Exception e) { return "Invalid action. Use ATTACK or INTERACT."; }
        if (ready() && MovementSync.INSTANCE.getWorld() != null) {
            Entity ent = MovementSync.INSTANCE.getWorld().getEntity(entityId);
            if (ent != null && ent.getPosition() != null) MovementSync.INSTANCE.lookAt(ent.getPosition());
        }
        Bot.INSTANCE.getSession().send(new ServerboundSwingPacket(Hand.MAIN_HAND));
        Bot.INSTANCE.getSession().send(action == InteractAction.ATTACK
                ? new ServerboundInteractPacket(entityId, action, false)
                : new ServerboundInteractPacket(entityId, action, Hand.MAIN_HAND, false));
        return "Interacted entity " + entityId + " (" + action + ")";
    }
}
