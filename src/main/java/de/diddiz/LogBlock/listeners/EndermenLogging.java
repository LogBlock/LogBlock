package de.diddiz.LogBlock.listeners;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import org.bukkit.entity.Enderman;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityChangeBlockEvent;

import static de.diddiz.LogBlock.config.Config.isLogging;

public class EndermenLogging extends LoggingListener {
    public EndermenLogging(LogBlock lb) {
        super(lb);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof Enderman && isLogging(event.getBlock().getWorld(), Logging.ENDERMEN)) {
            consumer.queueBlockReplace(new Actor("Enderman"), event.getBlock().getState(), event.getBlockData()); // Figure out how to get the data of the placed block;
        }
    }
}
