package de.diddiz.LogBlock.listeners;

import static de.diddiz.LogBlock.config.Config.getWorldConfig;
import static de.diddiz.LogBlock.config.Config.logCreeperExplosionsAsPlayerWhoTriggeredThese;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityExplodeEvent;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import de.diddiz.LogBlock.config.WorldConfig;

public class ExplosionLogging extends LoggingListener
{
	public ExplosionLogging(LogBlock lb) {
		super(lb);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onEntityExplode(EntityExplodeEvent event) {
		final WorldConfig wcfg = getWorldConfig(event.getLocation().getWorld());
		if (wcfg != null) {
			final String name;
			if (event.getEntity() == null) {
				if (!wcfg.isLogging(Logging.MISCEXPLOSION))
					return;
				name = "Explosion";
			} else if (event.getEntity() instanceof TNTPrimed) {
				if (!wcfg.isLogging(Logging.TNTEXPLOSION))
					return;
				name = "TNT";
			} else if (event.getEntity() instanceof Creeper) {
				if (!wcfg.isLogging(Logging.CREEPEREXPLOSION))
					return;
				if (logCreeperExplosionsAsPlayerWhoTriggeredThese) {
					final Entity target = ((Creeper)event.getEntity()).getTarget();
					name = target instanceof Player ? ((Player)target).getName() : "Creeper";
				} else
					name = "Creeper";
			} else if (event.getEntity() instanceof Fireball) {
				if (!wcfg.isLogging(Logging.GHASTFIREBALLEXPLOSION))
					return;
				name = "Ghast";
			} else if (event.getEntity() instanceof EnderDragon) {
				if (!wcfg.isLogging(Logging.ENDERDRAGON))
					return;
				name = "EnderDragon";
			} else if (event.getEntity() instanceof Wither) {
				if(!wcfg.isLogging(Logging.WITHER))
					return;
				name = "Wither";
			} else if (event.getEntity() instanceof WitherSkull) {
				if(!wcfg.isLogging(Logging.WITHER_SKULL))
					return;
				name = "WitherSkull";
			} else {
				if (!wcfg.isLogging(Logging.MISCEXPLOSION))
					return;
				name = "Explosion";
			}
			for (final Block block : event.blockList()) {
				final int type = block.getTypeId();
				if (wcfg.isLogging(Logging.SIGNTEXT) & (type == 63 || type == 68))
					consumer.queueSignBreak(name, (Sign)block.getState());
				else if (wcfg.isLogging(Logging.CHESTACCESS) && (type == 23 || type == 54 || type == 61))
					consumer.queueContainerBreak(name, block.getState());
				else
					consumer.queueBlockBreak(name, block.getState());
			}
		}
	}
}
