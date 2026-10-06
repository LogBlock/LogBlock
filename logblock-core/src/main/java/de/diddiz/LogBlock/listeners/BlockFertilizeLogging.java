package de.diddiz.LogBlock.listeners;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import de.diddiz.LogBlock.config.WorldConfig;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockFertilizeEvent;
import static de.diddiz.LogBlock.config.Config.getWorldConfig;

public class BlockFertilizeLogging extends LoggingListener {
    public BlockFertilizeLogging(LogBlock lb) {
        super(lb);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFertilize(BlockFertilizeEvent event) {
        final WorldConfig wcfg = getWorldConfig(event.getBlock().getLocation().getWorld());
        if (wcfg != null) {
            if (!wcfg.isLogging(Logging.BONEMEALSTRUCTUREGROW)) {
                return;
            }
            final Actor actor;
            if (event.getPlayer() != null) {
                actor = Actor.actorFromEntity(event.getPlayer());
            } else {
                actor = new Actor("Dispenser");
            }
            for (final BlockState state : event.getBlocks()) {
                consumer.queueBlockReplace(actor, state.getBlock().getState(), state);
            }
        }
    }
}
