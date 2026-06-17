package movementmcp;

import org.joml.Vector3d;
import xin.bbtt.MovementSync;
import xin.bbtt.mcbot.Bot;
import xin.bbtt.Entity.Entity;
import xin.bbtt.movements.DigBlockMovement;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerAction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.InteractAction;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.*;

public class ActionTools extends BaseMcpTools {

    @Override
    protected void init() {
        reg("interactEntity", "Interact with entity (ATTACK or INTERACT).", args -> {
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
        }, integer("entityId", "Entity ID"), str("action", "ATTACK or INTERACT"));

        reg("changeSlot", "Switch hotbar slot (0-8).", args -> {
            int slot = args.get("slot").getAsInt();
            if (slot < 0 || slot > 8) return "Slot must be 0-8.";
            Bot.INSTANCE.getSession().send(new ServerboundSetCarriedItemPacket(slot));
            return "Switched to slot " + slot;
        }, integer("slot", "Hotbar slot 0-8"));

        reg("useItem", "Use held item (right-click once).", args -> {
            Bot.INSTANCE.getSession().send(new ServerboundSwingPacket(Hand.MAIN_HAND));
            Bot.INSTANCE.getSession().send(new ServerboundUseItemPacket(Hand.MAIN_HAND, seq(), yaw(), pitch()));
            return "Used item.";
        });

        reg("useItemWithDuration", "Hold use item for duration (e.g. eat 1600ms, bow 1000ms).", args -> {
            long ms = args.get("durationMs").getAsLong();
            Bot.INSTANCE.getSession().send(new ServerboundSwingPacket(Hand.MAIN_HAND));
            Bot.INSTANCE.getSession().send(new ServerboundUseItemPacket(Hand.MAIN_HAND, seq(), yaw(), pitch()));
            new Thread(() -> {
                try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
                if (Bot.INSTANCE.getSession() != null)
                    Bot.INSTANCE.getSession().send(new ServerboundPlayerActionPacket(
                            PlayerAction.RELEASE_USE_ITEM, org.cloudburstmc.math.vector.Vector3i.from(0, 0, 0),
                            Direction.DOWN, seq()));
            }, "mcp-useitem-release").start();
            return "Using item for " + ms + "ms.";
        }, integer("durationMs", "Hold duration in milliseconds"));

        reg("releaseUseItem", "Release use item key.", args -> {
            Bot.INSTANCE.getSession().send(new ServerboundPlayerActionPacket(
                    PlayerAction.RELEASE_USE_ITEM, org.cloudburstmc.math.vector.Vector3i.from(0, 0, 0),
                    Direction.DOWN, seq()));
            return "Released item.";
        });

        reg("interactBlock", "Interact with block (place, press button, open chest, etc).", args -> {
            int x = args.get("x").getAsInt(), y = args.get("y").getAsInt(), z = args.get("z").getAsInt();
            Direction dir;
            try { dir = Direction.valueOf(args.get("direction").getAsString().toUpperCase()); }
            catch (Exception e) { return "Invalid direction. Use DOWN/UP/NORTH/SOUTH/WEST/EAST."; }
            Vector3d cur = pos();
            if (cur.distance(new Vector3d(x + 0.5, y + 0.5, z + 0.5)) > 6) return "Too far (max 6 blocks).";
            MovementSync.INSTANCE.lookAt(new Vector3d(x + 0.5, y + 0.5, z + 0.5));
            Bot.INSTANCE.getSession().send(new ServerboundSwingPacket(Hand.MAIN_HAND));
            Bot.INSTANCE.getSession().send(new ServerboundUseItemOnPacket(
                    org.cloudburstmc.math.vector.Vector3i.from(x, y, z), dir, Hand.MAIN_HAND,
                    0.5f, 0.5f, 0.5f, false, false, seq()));
            return String.format("Interacted block (%d,%d,%d).", x, y, z);
        }, integer("x"), integer("y"), integer("z"), str("direction", "Face: DOWN/UP/NORTH/SOUTH/WEST/EAST"));

        reg("mineBlock", "Mine block at coordinates.", args -> {
            int x = args.get("x").getAsInt(), y = args.get("y").getAsInt(), z = args.get("z").getAsInt();
            MovementSync.INSTANCE.lookAt(new Vector3d(x + 0.5, y + 0.5, z + 0.5));
            MovementSync.INSTANCE.getMovementController().addMovement(new DigBlockMovement(
                    org.cloudburstmc.math.vector.Vector3i.from(x, y, z)));
            return "Mining (" + x + "," + y + "," + z + ")";
        }, integer("x"), integer("y"), integer("z"));
    }
}
