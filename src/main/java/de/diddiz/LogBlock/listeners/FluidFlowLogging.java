package de.diddiz.LogBlock.listeners;

import static de.diddiz.LogBlock.config.Config.getWorldConfig;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockFromToEvent;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import de.diddiz.LogBlock.config.WorldConfig;

public class FluidFlowLogging extends LoggingListener
{
	private static final Set<Integer> nonFluidProofBlocks = new HashSet<Integer>(Arrays.asList(27, 28, 31, 32, 37, 38, 39, 40, 50, 51, 55, 59, 66, 69, 70, 75, 76, 78, 93, 94, 104, 105, 106));

	public FluidFlowLogging(LogBlock lb) {
		super(lb);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockFromTo(BlockFromToEvent event) {
		final WorldConfig wcfg = getWorldConfig(event.getBlock().getWorld());
		if (wcfg != null) {
			final Block to = event.getToBlock();
			final int typeFrom = event.getBlock().getTypeId();
			final int typeTo = to.getTypeId();
			final boolean canFlow = typeTo == 0 || nonFluidProofBlocks.contains(typeTo);
			if (typeFrom == 10 || typeFrom == 11) {
				if (canFlow && wcfg.isLogging(Logging.LAVAFLOW)) {
					if (isSurroundedByWater(to) && event.getBlock().getData() <= 2)
						consumer.queueBlockReplace("LavaFlow", to.getState(), 4, (byte)0);
					else if (typeTo == 0) {
						consumer.queueBlockPlace("LavaFlow", to.getLocation(), 10, (byte)(event.getBlock().getData() + 1));
					} else {
						consumer.queueBlockReplace("LavaFlow", to.getState(), 10, (byte)(event.getBlock().getData() + 1));
					}
				} else if (typeTo == 8 || typeTo == 9) {
					if (event.getFace() == BlockFace.DOWN) {
						consumer.queueBlockReplace("LavaFlow", to.getState(), 1, (byte)0);
					} else {
						consumer.queueBlockReplace("LavaFlow", to.getState(), 4, (byte)0);
					}
				}
			} else if ((typeFrom == 8 || typeFrom == 9) && wcfg.isLogging(Logging.WATERFLOW)) {
				if (typeTo == 0) {
					consumer.queueBlockPlace("WaterFlow", to.getLocation(), 8, (byte)(event.getBlock().getData() + 1));
				} else if (nonFluidProofBlocks.contains(typeTo)) {
					consumer.queueBlockReplace("WaterFlow", to.getState(), 8, (byte)(event.getBlock().getData() + 1));
				}
				else if (typeTo == 10 || typeTo == 11) {
					if (to.getData() == 0) {
						consumer.queueBlockReplace("WaterFlow", to.getState(), 49, (byte)0);
					} else if (event.getFace() == BlockFace.DOWN) {
						consumer.queueBlockReplace("LavaFlow", to.getState(), 1, (byte)0);
					}
				}
				if (typeTo == 0 || nonFluidProofBlocks.contains(typeTo)) {
					for (final BlockFace face : new BlockFace[]{BlockFace.DOWN, BlockFace.NORTH, BlockFace.WEST, BlockFace.EAST, BlockFace.SOUTH}) {
						final Block lower = to.getRelative(face);
						if (lower.getTypeId() == 10 || lower.getTypeId() == 11) {
							consumer.queueBlockReplace("WaterFlow", lower.getState(), lower.getData() == 0 ? 49 : 4, (byte)0);
						}
					}
				}
			}
		}
	}

	private static boolean isSurroundedByWater(Block block) {
		for (final BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.WEST, BlockFace.EAST, BlockFace.SOUTH}) {
			final int type = block.getRelative(face).getTypeId();
			if (type == 8 || type == 9)
				return true;
		}
		return false;
	}
}
