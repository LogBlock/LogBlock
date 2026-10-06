package de.diddiz.LogBlock.listeners;

import static de.diddiz.LogBlock.config.Config.isLogging;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockFormEvent;

public class OxidizationLogging extends LoggingListener {
    public OxidizationLogging(LogBlock lb) {
        super(lb);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPhysics(BlockFormEvent event) {
        if (isLogging(event.getBlock().getWorld(), Logging.OXIDIZATION)) {
            final Material type = event.getNewState().getType();
            if (type.name().contains("COPPER")) {
                consumer.queueBlockReplace(new Actor("NaturalOxidization"), event.getBlock().getState(), event.getNewState());
            }
        }
    }

}
