package de.diddiz.LogBlock.listeners;

import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockSpreadEvent;

import static de.diddiz.LogBlock.config.Config.isLogging;

public class BlockSpreadLogging extends LoggingListener
{

	public BlockSpreadLogging(LogBlock lb) {
		super(lb);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockSpread(BlockSpreadEvent event) {

		String name;

		World world  = event.getBlock().getWorld();
		Material type = event.getSource().getType();

		switch (type) {
			case GRASS:
				if (!isLogging(world, Logging.GRASSGROWTH)) return;
				name = "GrassGrowth";
				break;
			case MYCEL:
				if (!isLogging(world, Logging.MYCELIUMSPREAD)) return;
				name = "MyceliumSpread";
				break;
			case VINE:
				if (!isLogging(world, Logging.VINEGROWTH)) return;
				name = "VineGrowth";
				break;
			case RED_MUSHROOM:
			case BROWN_MUSHROOM:
				if (!isLogging(world, Logging.MUSHROOMSPREAD)) return;
				name = "MushroomSpread";
				break;
			default:
				return;
		}

		consumer.queueBlockReplace(name, event.getBlock().getState(), event.getNewState());
	}
}
