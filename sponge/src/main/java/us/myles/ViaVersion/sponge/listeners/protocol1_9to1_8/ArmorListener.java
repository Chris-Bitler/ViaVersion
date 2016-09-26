package us.myles.ViaVersion.sponge.listeners.protocol1_9to1_8;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.action.InteractEvent;
import org.spongepowered.api.event.entity.living.humanoid.player.RespawnPlayerEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.item.inventory.ClickInventoryEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;
import us.myles.ViaVersion.SpongePlugin;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.protocols.protocol1_9to1_8.ArmorType;
import us.myles.ViaVersion.protocols.protocol1_9to1_8.Protocol1_9TO1_8;
import us.myles.ViaVersion.sponge.listeners.ViaSpongeListener;

import java.util.Optional;
import java.util.UUID;

public class ArmorListener extends ViaSpongeListener {
    private static final UUID ARMOR_ATTRIBUTE = UUID.fromString("2AD3F246-FEE1-4E67-B886-69FD380BB150");

    public ArmorListener(SpongePlugin plugin) {
        super(plugin, Protocol1_9TO1_8.class);
    }

    //
    public void sendArmorUpdate(Player player) {
        // Ensure that the player is on our pipe
        if (!isOnPipe(player.getUniqueId())) return;


        int armor = 0;

        // TODO is there a method like getArmorContents?
        armor += calculate(player.getHelmet());
        armor += calculate(player.getChestplate());
        armor += calculate(player.getLeggings());
        armor += calculate(player.getBoots());

        PacketWrapper wrapper = new PacketWrapper(0x4B, null, getUserConnection(player.getUniqueId()));
        try {
            wrapper.write(Type.VAR_INT, getEntityId(player)); // Player ID
            wrapper.write(Type.INT, 1); // only 1 property
            wrapper.write(Type.STRING, "generic.armor");
            wrapper.write(Type.DOUBLE, 0D); //default 0 armor
            wrapper.write(Type.VAR_INT, 1); // 1 modifier
            wrapper.write(Type.UUID, ARMOR_ATTRIBUTE); // armor modifier uuid
            wrapper.write(Type.DOUBLE, (double) armor); // the modifier value
            wrapper.write(Type.BYTE, (byte) 0);// the modifier operation, 0 is add number

            wrapper.send(Protocol1_9TO1_8.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int calculate(Optional<ItemStack> itemStack) {
        if (itemStack.isPresent())
            return ArmorType.findByType(itemStack.get().getItem().getType().getId()).getArmorPoints();

        return 0;
    }

    @Listener
    public void onInventoryClick(ClickInventoryEvent e, @Root Player player) {
        for (SlotTransaction transaction : e.getTransactions()) {
            if (ArmorType.isArmor(transaction.getFinal().getType().getId()) ||
                    ArmorType.isArmor(e.getCursorTransaction().getFinal().getType().getId())) {
                sendDelayedArmorUpdate(player);
                break;
            }
        }
    }

    @Listener
    public void onInteract(InteractEvent event, @Root Player player) {
        if (player.getItemInHand().isPresent()) {
            if (ArmorType.isArmor(player.getItemInHand().get().getItem().getId()))
                sendDelayedArmorUpdate(player);
        }
    }

    @Listener
    public void onJoin(ClientConnectionEvent.Join e) {
        sendArmorUpdate(e.getTargetEntity());
    }

    @Listener
    public void onRespawn(RespawnPlayerEvent e) {
        if (!isOnPipe(e.getTargetEntity().getUniqueId())) return;

        sendDelayedArmorUpdate(e.getTargetEntity());
    }

    //  TODO find world change event
//    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
//    public void onWorldChange(PlayerChangedWorldEvent e) {
//        sendArmorUpdate(e.getPlayer());
//    }
//
    public void sendDelayedArmorUpdate(final Player player) {
        if (!isOnPipe(player.getUniqueId())) return; // Don't start a task if the player is not on the pipe
        Via.getPlatform().runSync(new Runnable() {
            @Override
            public void run() {
                sendArmorUpdate(player);
            }
        });
    }
}