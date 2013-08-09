package de.diddiz.LogBlock.listeners;

import static de.diddiz.LogBlock.config.Config.isLogging;
import static de.diddiz.LogBlock.config.Config.logKillsLevel;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import de.diddiz.LogBlock.config.Config.LogKillsLevel;

public class KillLogging extends LoggingListener
{

	public KillLogging(LogBlock lb) {
		super(lb);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onEntityDeath(EntityDeathEvent deathEvent) {
		EntityDamageEvent event = deathEvent.getEntity().getLastDamageCause();
		// For a death event, there should always be a damage event and it should not be cancelled.  Check anyway.
		if (event!= null && event.isCancelled() == false && isLogging(event.getEntity().getWorld(), Logging.KILL) && event instanceof EntityDamageByEntityEvent && event.getEntity() instanceof LivingEntity) {
			final LivingEntity victim = (LivingEntity)event.getEntity();
			final Entity killer = ((EntityDamageByEntityEvent)event).getDamager();
			if (logKillsLevel == LogKillsLevel.PLAYERS && !(victim instanceof Player && killer instanceof Player))
				return;
			else if (logKillsLevel == LogKillsLevel.MONSTERS && !((victim instanceof Player || victim instanceof Monster) && killer instanceof Player || killer instanceof Monster))
				return;
			consumer.queueKill(killer, victim);
		}
	}
}
