package de.diddiz.LogBlock.listeners;

import static de.diddiz.LogBlock.config.Config.isLogging;
import static de.diddiz.util.BukkitUtils.compareInventories;
import static de.diddiz.util.BukkitUtils.compressInventory;
import static de.diddiz.util.BukkitUtils.getInventoryHolderLocation;
import static de.diddiz.util.BukkitUtils.getInventoryHolderType;
import static de.diddiz.util.BukkitUtils.rawData;
import java.util.HashMap;
import java.util.Map;

import de.diddiz.LogBlock.Logging;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import de.diddiz.LogBlock.LogBlock;

public class ChestAccessLogging extends LoggingListener
{
	private final Map<HumanEntity, ItemStack[]> containers = new HashMap<HumanEntity, ItemStack[]>();

	public ChestAccessLogging(LogBlock lb) {
		super(lb);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onInventoryClose(InventoryCloseEvent event) {

		if (!isLogging(event.getPlayer().getWorld(), Logging.CHESTACCESS)) return;
		InventoryHolder holder = event.getInventory().getHolder();
		if (holder instanceof BlockState || holder instanceof DoubleChest) {
			final HumanEntity player = event.getPlayer();
			final ItemStack[] before = containers.get(player);
			if (before != null) {
				final ItemStack[] after = compressInventory(event.getInventory().getContents());
				final ItemStack[] diff = compareInventories(before, after);
				final Location loc = getInventoryHolderLocation(holder);
				for (final ItemStack item : diff) {
					consumer.queueChestAccess(player.getName(), loc, loc.getWorld().getBlockTypeIdAt(loc), (short)item.getTypeId(), (short)item.getAmount(), rawData(item));
				}
				containers.remove(player);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onInventoryOpen(InventoryOpenEvent event) {

		if (!isLogging(event.getPlayer().getWorld(), Logging.CHESTACCESS)) return;
		if (event.getInventory() != null) {
			InventoryHolder holder = event.getInventory().getHolder();
			if (holder instanceof BlockState || holder instanceof DoubleChest) {
				if (getInventoryHolderType(holder) != 58) {
					containers.put(event.getPlayer(), compressInventory(event.getInventory().getContents()));
				}
			}
		}
	}
}
