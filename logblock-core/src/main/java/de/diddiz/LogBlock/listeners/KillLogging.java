package de.diddiz.LogBlock.listeners;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import de.diddiz.LogBlock.config.Config.*;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDeathEvent;

import static de.diddiz.LogBlock.config.Config.*;

public class KillLogging extends LoggingListener {

    public KillLogging(LogBlock lb) {
        super(lb);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent deathEvent) {
        if (isLogging(deathEvent.getEntity().getWorld(), Logging.KILL)) {
            LivingEntity victim = deathEvent.getEntity();
            Entity killer = deathEvent.getDamageSource().getCausingEntity();
            if (killer != null) {
                if (logKillsLevel == LogKillsLevel.PLAYERS && !(victim instanceof Player && killer instanceof Player)) {
                    return;
                } else if (logKillsLevel == LogKillsLevel.MONSTERS && !((victim instanceof Player || victim instanceof Monster) && killer instanceof Player || killer instanceof Monster)) {
                    return;
                }
                consumer.queueKill(killer, victim);
            } else if (logEnvironmentalKills) {
                if (logKillsLevel == LogKillsLevel.PLAYERS && !(victim instanceof Player)) {
                    return;
                } else if (logKillsLevel == LogKillsLevel.MONSTERS && !((victim instanceof Player || victim instanceof Monster))) {
                    return;
                }
                NamespacedKey key = deathEvent.getDamageSource().getDamageType().getKey();
                Actor actor = new Actor(key == null ? "unknown" : key.getKey().toUpperCase());

                consumer.queueKill(actor, victim);
            }
        }
    }
}
