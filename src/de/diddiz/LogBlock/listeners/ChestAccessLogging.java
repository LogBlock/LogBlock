package de.diddiz.LogBlock.listeners;

import static de.diddiz.LogBlock.config.Config.isLogging;
import static de.diddiz.util.BukkitUtils.compareInventories;
import static de.diddiz.util.BukkitUtils.compressInventory;
import static de.diddiz.util.BukkitUtils.rawData;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;

public class ChestAccessLogging extends LoggingListener
{
	private final Map<Player, ContainerState> containers = new HashMap<Player, ContainerState>();

	public ChestAccessLogging(LogBlock lb) {
		super(lb);
	}

	public void checkInventoryClose(Player player) {
		final ContainerState cont = containers.get(player);
		if (cont != null) {
			final ItemStack[] before = cont.items;
			final BlockState state = cont.loc.getBlock().getState();
			if (!(state instanceof InventoryHolder))
				return;
			final ItemStack[] after = compressInventory(((InventoryHolder)state).getInventory().getContents());
			final ItemStack[] diff = compareInventories(before, after);
			for (final ItemStack item : diff)
				consumer.queueChestAccess(player.getName(), cont.loc, state.getTypeId(), (short)item.getTypeId(), (short)item.getAmount(), rawData(item));
			containers.remove(player);
		}
	}

	public void checkInventoryOpen(Player player, Block block) {
		final BlockState state = block.getState();
		if (!(state instanceof InventoryHolder))
			return;
		containers.put(player, new ContainerState(block.getLocation(), compressInventory(((InventoryHolder)state).getInventory().getContents())));
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerChat(PlayerChatEvent event) {
		checkInventoryClose(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		checkInventoryClose(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event) {
		checkInventoryClose(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		checkInventoryClose(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerInteract(PlayerInteractEvent event) {
		final Player player = event.getPlayer();
		checkInventoryClose(player);
		if (!event.isCancelled() && event.getAction() == Action.RIGHT_CLICK_BLOCK && isLogging(player.getWorld(), Logging.CHESTACCESS)) {
			final Block block = event.getClickedBlock();
			final int type = block.getTypeId();
			if (type == 23 || type == 54 || type == 61 || type == 62)
				checkInventoryOpen(player, block);
		}
	}

	private static class ContainerState
	{
		public final ItemStack[] items;
		public final Location loc;

		private ContainerState(Location loc, ItemStack[] items) {
			this.items = items;
			this.loc = loc;
		}
	}
}
