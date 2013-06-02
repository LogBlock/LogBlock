package de.diddiz.LogBlock.listeners;

import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import de.diddiz.LogBlock.config.WorldConfig;
import de.diddiz.util.BukkitUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityInteractEvent;

import static de.diddiz.LogBlock.config.Config.getWorldConfig;

public class CreatureInteractLogging extends LoggingListener
{
	public CreatureInteractLogging(LogBlock lb) {
		super(lb);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onEntityInteract(EntityInteractEvent event) {
		final WorldConfig wcfg = getWorldConfig(event.getEntity().getWorld());

		final EntityType entityType = event.getEntityType();

		// Mobs only
		if (event.getEntity() instanceof Player || entityType == null) return;

		if (wcfg != null) {
			final Block clicked = event.getBlock();
			final Material type = clicked.getType();
			final int typeId = type.getId();
			final byte blockData = clicked.getData();
			final Location loc = clicked.getLocation();

			switch (type) {
				case SOIL:
					if (wcfg.isLogging(Logging.CREATURECROPTRAMPLE)) {
						// 3 = Dirt ID
						consumer.queueBlock(entityType.getName(), loc, typeId, 3, blockData);
						// Log the crop on top as being broken
						Block trampledCrop = clicked.getRelative(BlockFace.UP);
						if (BukkitUtils.getCropBlocks().contains(trampledCrop.getType())) {
							consumer.queueBlockBreak("CreatureTrample", trampledCrop.getState());
						}
					}
					break;
			}
		}
	}
}

