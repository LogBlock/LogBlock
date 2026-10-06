package de.diddiz.LogBlock.listeners;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import de.diddiz.LogBlock.config.WorldConfig;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.world.StructureGrowEvent;

import static de.diddiz.LogBlock.config.Config.getWorldConfig;

public class StructureGrowLogging extends LoggingListener {
    public StructureGrowLogging(LogBlock lb) {
        super(lb);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStructureGrow(StructureGrowEvent event) {
        final WorldConfig wcfg = getWorldConfig(event.getWorld());
        if (wcfg != null) {
            if (!wcfg.isLogging(Logging.NATURALSTRUCTUREGROW)) {
                return;
            }
            if (!event.isFromBonemeal()) {
                final Actor actor = new Actor("NaturalGrow");
                for (final BlockState state : event.getBlocks()) {
                    consumer.queueBlockReplace(actor, state.getBlock().getState(), state);
                }
            }
        }
    }
}
