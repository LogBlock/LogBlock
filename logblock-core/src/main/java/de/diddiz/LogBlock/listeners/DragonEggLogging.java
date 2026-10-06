package de.diddiz.LogBlock.listeners;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import de.diddiz.LogBlock.config.Config;
import de.diddiz.LogBlock.util.LoggingUtil;

public class DragonEggLogging extends LoggingListener {

    private UUID lastDragonEggInteractionPlayer;
    private Location lastDragonEggInteractionLocation;

    public DragonEggLogging(LogBlock lb) {
        super(lb);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.hasBlock() && event.getClickedBlock().getType() == Material.DRAGON_EGG) {
            Block block = event.getClickedBlock();
            if (!Config.isLogging(block.getWorld(), Logging.DRAGONEGGTELEPORT)) {
                return;
            }
            lastDragonEggInteractionPlayer = event.getPlayer().getUniqueId();
            lastDragonEggInteractionLocation = block.getLocation();
            new BukkitRunnable() {
                @Override
                public void run() {
                    lastDragonEggInteractionPlayer = null;
                    lastDragonEggInteractionLocation = null;
                }
            }.runTask(LogBlock.getInstance());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDragonEggTeleport(BlockFromToEvent event) {
        Block block = event.getBlock();
        Player teleportCause = null;
        if (lastDragonEggInteractionPlayer != null && lastDragonEggInteractionLocation != null && lastDragonEggInteractionLocation.equals(block.getLocation())) {
            teleportCause = Bukkit.getPlayer(lastDragonEggInteractionPlayer);
        }

        if (block.getType() == Material.DRAGON_EGG && Config.isLogging(block.getWorld(), Logging.DRAGONEGGTELEPORT)) {
            Actor actor = new Actor("DragonEgg");
            if (teleportCause != null) {
                actor = Actor.actorFromEntity(teleportCause);
            }
            BlockData data = block.getBlockData();
            consumer.queueBlockBreak(actor, block.getLocation(), data);
            BlockState finalState = event.getToBlock().getState();
            finalState.setBlockData(data);
            LoggingUtil.smartLogBlockPlace(consumer, actor, event.getToBlock().getState(), finalState);
        }
    }
}
