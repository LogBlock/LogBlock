package de.diddiz.LogBlock.listeners;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityChangeBlockEvent;

import static de.diddiz.LogBlock.config.Config.isLogging;

public class EntityChangeBlockLogging extends LoggingListener {
    public EntityChangeBlockLogging(LogBlock lb) {
        super(lb);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof Wither) {
            if (isLogging(event.getBlock().getWorld(), Logging.WITHER)) {
                consumer.queueBlockReplace(Actor.actorFromEntity(event.getEntity()), event.getBlock().getState(), event.getBlockData());
            }
        } else if (event.getEntity() instanceof Enderman) {
            if (isLogging(event.getBlock().getWorld(), Logging.ENDERMEN)) {
                consumer.queueBlockReplace(Actor.actorFromEntity(event.getEntity()), event.getBlock().getState(), event.getBlockData());
            }
        } else if (event.getEntity() instanceof Sheep) {
            if (isLogging(event.getBlock().getWorld(), Logging.GRASS_EAT)) {
                consumer.queueBlockReplace(Actor.actorFromEntity(event.getEntity()), event.getBlock().getState(), event.getBlockData());
            }
        } else {
            if (isLogging(event.getBlock().getWorld(), event.getEntity() instanceof Player ? Logging.BLOCKPLACE : Logging.MISCENTITYCHANGEBLOCK)) {
                consumer.queueBlockReplace(Actor.actorFromEntity(event.getEntity()), event.getBlock().getState(), event.getBlockData());
            }
        }
    }
}
