package de.diddiz.worldedit;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalPlayer;
import com.sk89q.worldedit.LocalWorld;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bags.BlockBag;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.bukkit.BukkitWorld;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import de.diddiz.LogBlock.config.Config;

public class LogBlockEditSession extends EditSession {

	private LocalPlayer player;
	private Actor actor;
	private LogBlock plugin;

	/**
	 * {@inheritDoc}
	 */
	public LogBlockEditSession(LocalWorld world, int maxBlocks, LocalPlayer player, LogBlock lb) {
		super(world, maxBlocks);
		this.player = player;
		this.plugin = lb;
		this.actor = Actor.actorFromEntity(plugin.getServer().getPlayer(player.getName()));
	}

	/**
	 * {@inheritDoc}
	 */
	public LogBlockEditSession(LocalWorld world, int maxBlocks, BlockBag blockBag, LocalPlayer player, LogBlock lb) {
		super(world, maxBlocks, blockBag);
		this.player = player;
		this.plugin = lb;
		this.actor = Actor.actorFromEntity(plugin.getServer().getPlayer(player.getName()));
	}

	@Override
	public boolean rawSetBlock(Vector pt, BaseBlock block) {
		if (!(player.getWorld() instanceof BukkitWorld) || !(Config.isLogging(player.getWorld().getName(), Logging.WORLDEDIT))) {
			return super.rawSetBlock(pt, block);
		}

		int typeBefore = ((BukkitWorld) player.getWorld()).getWorld().getBlockTypeIdAt(pt.getBlockX(), pt.getBlockY(), pt.getBlockZ());
		byte dataBefore = ((BukkitWorld) player.getWorld()).getWorld().getBlockAt(pt.getBlockX(), pt.getBlockY(), pt.getBlockZ()).getData();
		// If we're dealing with a sign, store the block state to read the text off
		BlockState stateBefore = null;
		if (typeBefore == Material.SIGN_POST.getId() || typeBefore == Material.SIGN.getId()) {
			stateBefore = ((BukkitWorld) player.getWorld()).getWorld().getBlockAt(pt.getBlockX(), pt.getBlockY(), pt.getBlockZ()).getState();
		}
		boolean success = super.rawSetBlock(pt, block);
		if (success) {
			Location location = new Location(((BukkitWorld) player.getWorld()).getWorld(), pt.getBlockX(), pt.getBlockY(), pt.getBlockZ());

			// Check to see if we've broken a sign
			if (Config.isLogging(location.getWorld().getName(), Logging.SIGNTEXT) && (typeBefore == Material.SIGN_POST.getId() || typeBefore == Material.SIGN.getId())) {
				plugin.getConsumer().queueSignBreak(actor, (Sign) stateBefore);
				if (block.getType() != Material.AIR.getId()) {
					plugin.getConsumer().queueBlockPlace(actor, location, block.getType(), (byte) block.getData());
				}
			} else {
				if (dataBefore != 0) {
					plugin.getConsumer().queueBlockBreak(actor, location, typeBefore, dataBefore);
					if (block.getType() != Material.AIR.getId()) {
						plugin.getConsumer().queueBlockPlace(actor, location, block.getType(), (byte) block.getData());
					}
				} else {
					plugin.getConsumer().queueBlock(actor, location, typeBefore, block.getType(), (byte) block.getData());
				}
			}
		}
		return success;
	}

}
