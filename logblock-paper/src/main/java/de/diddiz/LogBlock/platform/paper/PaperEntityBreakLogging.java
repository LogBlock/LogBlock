package de.diddiz.LogBlock.platform.paper;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.listeners.AdvancedEntityLogging;
import de.diddiz.LogBlock.util.LoggingUtil;
import io.papermc.paper.event.entity.EntityBreakByEntityEvent;
import io.papermc.paper.event.entity.EntityBreakEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class PaperEntityBreakLogging implements Listener {
    private final AdvancedEntityLogging logging;

    public PaperEntityBreakLogging(AdvancedEntityLogging logging) {
        this.logging = logging;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityBreak(EntityBreakEvent event) {
        Entity entity = event.getEntity();
        if (event.isCancelled() || entity.getType() != EntityType.CUSHION) {
            return;
        }

        Actor actor;
        if (event instanceof EntityBreakByEntityEvent byEntity) {
            actor = Actor.actorFromEntity(LoggingUtil.getRealDamager(byEntity.getRemover()));
        } else {
            actor = new Actor(event.getCause().toString());
        }
        logging.logEntityBreak(entity, actor);
    }
}
