package de.diddiz.worldedit;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.logging.AbstractLoggingExtent;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import de.diddiz.LogBlock.config.Config;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;

public class WorldEditLoggingHook {

	private LogBlock plugin;

	public WorldEditLoggingHook(LogBlock plugin) {
		this.plugin = plugin;
	}

	public void hook() {
		WorldEdit.getInstance().getEventBus().register(new Object() {
			@Subscribe
			public void wrapForLogging(final EditSessionEvent event) {
				final Actor actor = event.getActor();
				if (actor == null || !(actor instanceof Player)) return;

				// Check to ensure the world should be logged
				String worldName = event.getWorld().getName();
				// If config becomes reloadable, this check should be moved
				if (!(Config.isLogging(worldName, Logging.WORLDEDIT))) {
					return;
				}

				final org.bukkit.World bukkitWorld = Bukkit.getWorld(worldName);
				if (bukkitWorld == null) {
					return;
				}

				event.setExtent(new AbstractLoggingExtent(event.getExtent()) {
					@Override
					protected void onBlockChange(Vector pt, BaseBlock block) {

						if (event.getStage() != EditSession.Stage.BEFORE_CHANGE) {
							return;
						}

						Location location = new Location(bukkitWorld, pt.getBlockX(), pt.getBlockY(), pt.getBlockZ());

						Block origin = location.getBlock();
						int typeBefore = origin.getTypeId();
						byte dataBefore = origin.getData();
						// If we're dealing with a sign, store the block state to read the text off
						BlockState stateBefore = null;
						if (typeBefore == Material.SIGN_POST.getId() || typeBefore == Material.SIGN.getId()) {
							stateBefore = origin.getState();
						}

						// Check to see if we've broken a sign
						if (Config.isLogging(location.getWorld().getName(), Logging.SIGNTEXT) && (typeBefore == Material.SIGN_POST.getId() || typeBefore == Material.SIGN.getId())) {
							plugin.getConsumer().queueSignBreak(actor.getName(), (Sign) stateBefore);
							if (block.getType() != Material.AIR.getId()) {
								plugin.getConsumer().queueBlockPlace(actor.getName(), location, block.getType(), (byte) block.getData());
							}
						} else {
							if (dataBefore != 0) {
								plugin.getConsumer().queueBlockBreak(actor.getName(), location, typeBefore, dataBefore);
								if (block.getType() != Material.AIR.getId()) {
									plugin.getConsumer().queueBlockPlace(actor.getName(), location, block.getType(), (byte) block.getData());
								}
							} else {
								plugin.getConsumer().queueBlock(actor.getName(), location, typeBefore, block.getType(), (byte) block.getData());
							}
						}
					}
				});
			}
		});
	}
}