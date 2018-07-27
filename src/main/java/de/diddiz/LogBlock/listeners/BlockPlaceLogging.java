package de.diddiz.LogBlock.listeners;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import de.diddiz.LogBlock.config.WorldConfig;
import de.diddiz.util.BukkitUtils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;

import static de.diddiz.LogBlock.config.Config.getWorldConfig;
import static de.diddiz.LogBlock.config.Config.isLogging;

public class BlockPlaceLogging extends LoggingListener {
    public BlockPlaceLogging(LogBlock lb) {
        super(lb);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        final WorldConfig wcfg = getWorldConfig(event.getBlock().getWorld());
        if (wcfg != null && wcfg.isLogging(Logging.BLOCKPLACE)) {
            final Material type = event.getBlock().getType();
            final BlockState before = event.getBlockReplacedState();
            final BlockState after = event.getBlockPlaced().getState();
            final Actor actor = Actor.actorFromEntity(event.getPlayer());

            //Handle falling blocks
            if (type.hasGravity()) {

                // Catch placed blocks overwriting something
                if (!BukkitUtils.isEmpty(before.getType())) {
                    consumer.queueBlockBreak(actor, before);
                }

                Location loc = event.getBlock().getLocation();
                int x = loc.getBlockX();
                int y = loc.getBlockY();
                int z = loc.getBlockZ();
                // Blocks only fall if they have a chance to start a velocity
                if (BukkitUtils.isEmpty(event.getBlock().getRelative(BlockFace.DOWN).getType())) {
                    while (y > 0 && BukkitUtils.canFall(loc.getWorld(), x, (y - 1), z)) {
                        y--;
                    }
                }
                // If y is 0 then the sand block fell out of the world :(
                if (y != 0) {
                    Location finalLoc = new Location(loc.getWorld(), x, y, z);
                    // Run this check to avoid false positives
                    if (!BukkitUtils.getFallingEntityKillers().contains(finalLoc.getBlock().getType())) {
                        if (BukkitUtils.isEmpty(finalLoc.getBlock().getType()) || finalLoc.equals(event.getBlock().getLocation())) {
                            consumer.queueBlockPlace(actor, finalLoc, event.getBlock().getBlockData());
                        } else {
                            consumer.queueBlockReplace(actor, finalLoc, finalLoc.getBlock().getBlockData(), event.getBlock().getBlockData());
                        }
                    }
                }
                return;
            }

            //Sign logging is handled elsewhere
            if (wcfg.isLogging(Logging.SIGNTEXT) && (type == Material.SIGN || type == Material.WALL_SIGN)) {
                return;
            }

            //Delay queuing by one tick to allow data to be updated
            LogBlock.getInstance().getServer().getScheduler().scheduleSyncDelayedTask(LogBlock.getInstance(), new Runnable() {
                @Override
                public void run() {
                    if (BukkitUtils.isEmpty(before.getType())) {
                        consumer.queueBlockPlace(actor, after);
                    } else {
                        consumer.queueBlockReplace(actor, before, after);
                    }
                }
            }, 1L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        if (isLogging(event.getPlayer().getWorld(), Logging.BLOCKPLACE)) {
            consumer.queueBlockPlace(Actor.actorFromEntity(event.getPlayer()), event.getBlockClicked().getRelative(event.getBlockFace()).getLocation(), Bukkit.createBlockData(event.getBucket() == Material.WATER_BUCKET ? Material.WATER : Material.LAVA));
        }
    }
}
