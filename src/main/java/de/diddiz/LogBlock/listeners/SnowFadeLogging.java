package de.diddiz.LogBlock.listeners;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockFadeEvent;

import static de.diddiz.LogBlock.config.Config.isLogging;

public class SnowFadeLogging extends LoggingListener {
    public SnowFadeLogging(LogBlock lb) {
        super(lb);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event) {
        if (isLogging(event.getBlock().getWorld(), Logging.SNOWFADE)) {
            final Material type = event.getBlock().getType();
            if (type == Material.SNOW || type == Material.ICE) {
                consumer.queueBlockReplace(new Actor("SnowFade"), event.getBlock().getState(), event.getNewState());
            }
        }
    }
}
