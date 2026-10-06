package de.diddiz.LogBlock.listeners;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import de.diddiz.LogBlock.config.Config;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.CauldronLevelChangeEvent;

public class CauldronLogging extends LoggingListener {
    public CauldronLogging(LogBlock lb) {
        super(lb);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCauldronLevelChange(CauldronLevelChangeEvent event) {
        if (Config.isLogging(event.getBlock().getWorld(), Logging.CAULDRONINTERACT)) {
            Entity causingEntity = event.getEntity();
            if (causingEntity instanceof Player) {
                consumer.queueBlockReplace(Actor.actorFromEntity(causingEntity), event.getBlock().getBlockData(), event.getNewState());
            }
        }
    }
}
