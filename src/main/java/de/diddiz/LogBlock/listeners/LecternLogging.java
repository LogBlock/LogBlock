package de.diddiz.LogBlock.listeners;

import static de.diddiz.LogBlock.config.Config.getWorldConfig;
import static de.diddiz.LogBlock.config.Config.isLogging;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import de.diddiz.LogBlock.config.WorldConfig;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.Lectern;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class LecternLogging extends LoggingListener {
    public LecternLogging(LogBlock lb) {
        super(lb);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        final Block clicked = event.getClickedBlock();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND || !event.hasBlock() || clicked == null) {
            return;
        }
        final Player player = event.getPlayer();
        if (!isLogging(player.getWorld(), Logging.LECTERNBOOKCHANGE)) {
            return;
        }
        final Material type = clicked.getType();
        if (type == Material.LECTERN) {
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (mainHand != null && mainHand.getType() != Material.AIR && Tag.ITEMS_LECTERN_BOOKS.isTagged(mainHand.getType()) && clicked.getState() instanceof Lectern lectern) {
                ItemStack currentInLectern = lectern.getSnapshotInventory().getItem(0);
                if (currentInLectern == null || currentInLectern.getType() == Material.AIR) {
                    ItemStack stack = mainHand.clone();
                    stack.setAmount(1);
                    Lectern newLectern = (Lectern) clicked.getState();
                    newLectern.getSnapshotInventory().setItem(0, stack);
                    org.bukkit.block.data.type.Lectern blockDataOld = (org.bukkit.block.data.type.Lectern) newLectern.getBlockData();
                    org.bukkit.block.data.type.Lectern blockDataWithBook = (org.bukkit.block.data.type.Lectern) Bukkit.createBlockData("lectern[has_book=true]");
                    blockDataWithBook.setFacing(blockDataOld.getFacing());
                    blockDataWithBook.setPowered(blockDataOld.isPowered());
                    newLectern.setBlockData(blockDataWithBook);
                    consumer.queueBlockReplace(Actor.actorFromEntity(event.getPlayer()), lectern, newLectern);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTakeLecternBook(PlayerTakeLecternBookEvent event) {
        final WorldConfig wcfg = getWorldConfig(event.getPlayer().getWorld());
        if (wcfg != null && wcfg.isLogging(Logging.LECTERNBOOKCHANGE)) {
            Lectern oldState = event.getLectern();
            Lectern newState = (Lectern) oldState.getBlock().getState();
            try {
                newState.getSnapshotInventory().setItem(0, null);
            } catch (NullPointerException e) {
                //ignored
            }
            org.bukkit.block.data.type.Lectern oldBlockData = (org.bukkit.block.data.type.Lectern) oldState.getBlockData();
            org.bukkit.block.data.type.Lectern blockData = (org.bukkit.block.data.type.Lectern) Material.LECTERN.createBlockData();
            blockData.setFacing(oldBlockData.getFacing());
            blockData.setPowered(oldBlockData.isPowered());
            newState.setBlockData(blockData);
            consumer.queueBlockReplace(Actor.actorFromEntity(event.getPlayer()), oldState, newState);
        }
    }
}
