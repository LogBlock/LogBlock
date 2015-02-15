package de.diddiz.LogBlock.listeners;

import static de.diddiz.LogBlock.config.Config.getWorldConfig;
import static de.diddiz.LogBlock.config.Config.logCreeperExplosionsAsPlayerWhoTriggeredThese;
import static de.diddiz.util.BukkitUtils.getContainerBlocks;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkull;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.projectiles.ProjectileSource;

import de.diddiz.LogBlock.Actor;
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
			Actor actor = new Actor("Explosion");
			Entity source = event.getEntity();
			if (source == null) {
				if (!wcfg.isLogging(Logging.MISCEXPLOSION))
					return;
			} else if (source instanceof TNTPrimed) {
				if (!wcfg.isLogging(Logging.TNTEXPLOSION))
					return;
				actor = new Actor("TNT");
			} else if (source instanceof ExplosiveMinecart) {
				if (!wcfg.isLogging(Logging.TNTMINECARTEXPLOSION))
					return;
				actor = new Actor("TNTMinecart");
			} else if (source instanceof Creeper) {
				if (!wcfg.isLogging(Logging.CREEPEREXPLOSION))
					return;
				if (logCreeperExplosionsAsPlayerWhoTriggeredThese) {
					final Entity target = ((Creeper) source).getTarget();
					actor = target instanceof Player ? Actor.actorFromEntity(target) : new Actor("Creeper");
				} else
					new Actor("Creeper");
			} else if (source instanceof Fireball) {
				Fireball fireball = (Fireball) source;
				ProjectileSource shooter = fireball.getShooter();
				if (shooter == null) {
					return;
				}
				if (shooter instanceof Ghast) {
					if (!wcfg.isLogging(Logging.GHASTFIREBALLEXPLOSION)) {
						return;
					}
					actor = Actor.actorFromProjectileSource(shooter);
				} else if (shooter instanceof Wither) {
					if (!wcfg.isLogging(Logging.WITHER)) {
						return;
					}
					actor = Actor.actorFromProjectileSource(shooter);
				}
			} else if (source instanceof EnderDragon) {
				if (!wcfg.isLogging(Logging.ENDERDRAGON))
					return;
				actor = Actor.actorFromEntity(source);
			} else if (source instanceof Wither) {
				if(!wcfg.isLogging(Logging.WITHER))
					return;
				actor = Actor.actorFromEntity(source);
			} else if (source instanceof WitherSkull) {
				if(!wcfg.isLogging(Logging.WITHER_SKULL))
					return;
				actor = Actor.actorFromEntity(source);
			} else {
				if (!wcfg.isLogging(Logging.MISCEXPLOSION))
					return;
			}
			for (final Block block : event.blockList()) {
				final int type = block.getTypeId();
				if (wcfg.isLogging(Logging.SIGNTEXT) & (type == 63 || type == 68))
					consumer.queueSignBreak(actor, (Sign)block.getState());
				else if (wcfg.isLogging(Logging.CHESTACCESS) && (getContainerBlocks().contains(Material.getMaterial(type))))
					consumer.queueContainerBreak(actor, block.getState());
				else
					consumer.queueBlockBreak(actor, block.getState());
			}
		}
	}
}
