package de.diddiz.LogBlock.listeners;

import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import de.diddiz.LogBlock.config.WorldConfig;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityExplodeEvent;

import static de.diddiz.LogBlock.config.Config.getWorldConfig;
import static de.diddiz.LogBlock.config.Config.logCreeperExplosionsAsPlayerWhoTriggeredThese;

public class ExplosionLogging extends LoggingListener
{
	public ExplosionLogging(LogBlock lb) {
		super(lb);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onEntityExplode(EntityExplodeEvent event) {
		final WorldConfig wcfg = getWorldConfig(event.getLocation().getWorld());
		if (wcfg != null) {
			String name = "Explosion";
			Entity source = event.getEntity();
			if (source == null) {
				if (!wcfg.isLogging(Logging.MISCEXPLOSION))
					return;
			} else if (source instanceof TNTPrimed) {
				if (!wcfg.isLogging(Logging.TNTEXPLOSION))
					return;
				name = "TNT";
			} else if (source instanceof ExplosiveMinecart) {
				if (!wcfg.isLogging(Logging.TNTMINECARTEXPLOSION))
					return;
				name = "TNTMinecart";
			} else if (source instanceof Creeper) {
				if (!wcfg.isLogging(Logging.CREEPEREXPLOSION))
					return;
				if (logCreeperExplosionsAsPlayerWhoTriggeredThese) {
					final Entity target = ((Creeper) source).getTarget();
					name = target instanceof Player ? ((Player)target).getName() : "Creeper";
				} else
					name = "Creeper";
			} else if (source instanceof Fireball) {
				Fireball fireball = (Fireball) source;
				Entity shooter = fireball.getShooter();
				if (shooter == null) {
					return;
				}
				if (shooter instanceof Ghast) {
					if (!wcfg.isLogging(Logging.GHASTFIREBALLEXPLOSION)) {
						return;
					}
					name = "Ghast";
				} else if (shooter instanceof Wither) {
					if (!wcfg.isLogging(Logging.WITHER)) {
						return;
					}
					name = "Wither";
				}
			} else if (source instanceof EnderDragon) {
				if (!wcfg.isLogging(Logging.ENDERDRAGON))
					return;
				name = "EnderDragon";
			} else if (source instanceof Wither) {
				if(!wcfg.isLogging(Logging.WITHER))
					return;
				name = "Wither";
			} else if (source instanceof WitherSkull) {
				if(!wcfg.isLogging(Logging.WITHER_SKULL))
					return;
				name = "WitherSkull";
			} else {
				if (!wcfg.isLogging(Logging.MISCEXPLOSION))
					return;
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
