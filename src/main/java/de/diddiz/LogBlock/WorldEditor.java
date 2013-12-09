package de.diddiz.LogBlock;

import de.diddiz.util.BukkitUtils;
import de.diddiz.util.serializable.itemstack.SerializableItemStack;
import de.diddiz.util.serializable.itemstack.SerializableItemStackFactory;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Bed;
import org.bukkit.material.PistonBaseMaterial;
import org.bukkit.material.PistonExtensionMaterial;

import java.io.File;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

import static de.diddiz.LogBlock.config.Config.dontRollback;
import static de.diddiz.LogBlock.config.Config.replaceAnyway;
import static de.diddiz.util.BukkitUtils.equalTypes;
import static de.diddiz.util.BukkitUtils.saveSpawnHeight;
import static de.diddiz.util.MaterialName.materialName;
import static org.bukkit.Bukkit.getLogger;

public class WorldEditor implements Runnable
{
	private final LogBlock logblock;
	private final Queue<Edit> edits = new LinkedBlockingQueue<Edit>();
	private final World world;

    /**
     * The player responsible for editing the world, used to report progress
     */
    private CommandSender sender;
	private int taskID;
	private int successes = 0, blacklistCollisions = 0;
	private long elapsedTime = 0;
	public LookupCacheElement[] errors;

	public WorldEditor(LogBlock logblock, World world) {
		this.logblock = logblock;
		this.world = world;
	}

	public int getSize() {
		return edits.size();
	}

	public int getSuccesses() {
		return successes;
	}

	public int getErrors() {
		return errors.length;
	}

	public int getBlacklistCollisions() {
		return blacklistCollisions;
	}


    public void setSender(CommandSender sender) {
        this.sender = sender;
    }

	public void queueRollbackEdit(int x, int y, int z, int replaced, int type, byte data, String signtext, SerializableItemStack itemStack) {
		edits.add(new RevertEdit(0, new Location(world, x, y, z), null, replaced, type, data, signtext, itemStack));
	}

	public void queueRedoEdit(int x, int y, int z, int replaced, int type, byte data, String signtext, SerializableItemStack itemStack) {
		edits.add(new RestoreEdit(0, new Location(world, x, y, z), null, replaced, type, data, signtext, itemStack));
	}

	public long getElapsedTime() {
		return elapsedTime;
	}

	synchronized public void start() throws Exception {
		final long start = System.currentTimeMillis();
		taskID = logblock.getServer().getScheduler().scheduleSyncRepeatingTask(logblock, this, 0, 1);
		if (taskID == -1)
			throw new Exception("Failed to schedule task");
		try {
			this.wait();
		} catch (final InterruptedException ex) {
			throw new Exception("Interrupted");
		}
		elapsedTime = System.currentTimeMillis() - start;
	}

	@Override
	public synchronized void run() {
		final List<WorldEditorException> errorList = new ArrayList<WorldEditorException>();
		int counter = 0;
        float size = edits.size();
		while (!edits.isEmpty() && counter < 100) {
			try {
				switch (edits.poll().perform()) {
					case SUCCESS:
						successes++;
						break;
					case BLACKLISTED:
						blacklistCollisions++;
						break;
				}
			} catch (final WorldEditorException ex) {
				errorList.add(ex);
			} catch (final Exception ex) {
				getLogger().log(Level.WARNING, "[WorldEditor] Exeption: ", ex);
			}
			counter++;
            if (sender != null) {
                float percentage = ((size - edits.size()) / size) * 100.0F;
                if (percentage % 20 == 0) {
                    sender.sendMessage(ChatColor.GOLD + "[LogBlock]" + ChatColor.YELLOW + " Rollback progress: " + percentage + "%" +
                            " Blocks edited: " + counter);
                }
            }
		}
		if (edits.isEmpty()) {
			logblock.getServer().getScheduler().cancelTask(taskID);
			if (errorList.size() > 0)
				try {
					final File file = new File("plugins/LogBlock/error/WorldEditor-" + new SimpleDateFormat("yy-MM-dd-HH-mm-ss").format(System.currentTimeMillis()) + ".log");
					file.getParentFile().mkdirs();
					final PrintWriter writer = new PrintWriter(file);
					for (final LookupCacheElement err : errorList)
						writer.println(err.getMessage());
					writer.close();
				} catch (final Exception ex) {
				}
			errors = errorList.toArray(new WorldEditorException[errorList.size()]);
			notify();
		}
	}

	private static enum PerformResult
	{
		SUCCESS, BLACKLISTED, NO_ACTION
	}

	private abstract class Edit extends BlockChange {

		public Edit(long time, Location loc, String playerName, int replaced, int type, byte data, String signtext, SerializableItemStack itemStack) {
			super(time, loc, playerName, replaced, type, data, signtext, itemStack);
		}

		public PerformResult perform() throws WorldEditorException {

			// Should we skip this edit?
			if (!checkBlackList()) return PerformResult.BLACKLISTED;
			final Block block = loc.getBlock();
			final BlockState state = block.getState();
			if (!checkAction(block)) return PerformResult.NO_ACTION;

			// Ensure the chunk that's going to be edited is loaded
			if (!world.isChunkLoaded(state.getChunk())) world.loadChunk(state.getChunk());
			if (afterID == beforeID) {
				return performChest(block, state);
			}

			// Are these types actually equal?
			if (!checkEqual(block)) return PerformResult.NO_ACTION;

			// Begin editing
			return performBlockEdit(block, state);
		}

		public PerformResult performBlockEdit(Block block, BlockState state) throws WorldEditorException {
			// Prevent errors
			destroyHolder(state);
			if (!ambiguousA(block)) return PerformResult.NO_ACTION;

			// TODO break this down into individual methods
			final int curtype = block.getTypeId();
			if (signtext != null && (curtype == 63 || curtype == 68)) {
				final Sign sign = (Sign)block.getState();
				final String[] lines = signtext.split("\0", 4);
				if (lines.length < 4)
					return PerformResult.NO_ACTION;
				for (int i = 0; i < 4; i++)
					sign.setLine(i, lines[i]);
				if (!sign.update())
					throw new WorldEditorException("Failed to update signtext of " + materialName(block.getTypeId()), block.getLocation());
			} else if (curtype == 26) {
				final Bed bed = (Bed)block.getState().getData();
				final Block secBlock = bed.isHeadOfBed() ? block.getRelative(bed.getFacing().getOppositeFace()) : block.getRelative(bed.getFacing());
				if (secBlock.getTypeId() == 0 && !secBlock.setTypeIdAndData(26, (byte)(bed.getData() | 8), true))
					throw new WorldEditorException(secBlock.getTypeId(), 26, secBlock.getLocation());
			} else if ((curtype == 29 || curtype == 33) && (block.getData() & 8) > 0) {
				final PistonBaseMaterial piston = (PistonBaseMaterial)block.getState().getData();
				final Block secBlock = block.getRelative(piston.getFacing());
				if (secBlock.getTypeId() == 0 && !secBlock.setTypeIdAndData(34, curtype == 29 ? (byte)(block.getData() | 8) : (byte)(block.getData() & ~8), true))
					throw new WorldEditorException(secBlock.getTypeId(), 34, secBlock.getLocation());
			} else if (curtype == 34) {
				final PistonExtensionMaterial piston = (PistonExtensionMaterial)block.getState().getData();
				final Block secBlock = block.getRelative(piston.getFacing().getOppositeFace());
				if (secBlock.getTypeId() == 0 && !secBlock.setTypeIdAndData(piston.isSticky() ? 29 : 33, (byte)(block.getData() | 8), true))
					throw new WorldEditorException(secBlock.getTypeId(), piston.isSticky() ? 29 : 33, secBlock.getLocation());
			} else if (curtype == 18 && (block.getData() & 8) > 0)
				block.setData((byte)(block.getData() & 0xF7));
			return PerformResult.SUCCESS;
		}

		/**
		 *
		 * @return true if the black list is fine
		 */
		public abstract boolean checkBlackList();

		/**
		 *
		 * @return true if something is going to be done
		 */
		public abstract boolean checkAction(Block block);

		/**
		 *
		 * @return true if something was done
		 */
		public abstract PerformResult performChest(Block block, BlockState state) throws WorldEditorException;

		/**
		 *
		 * @return true if the blocks are different
		 */
		public abstract boolean checkEqual(Block block);

		/**
		 * Ensures that the block will not cause an exception when edited over
		 */
		public abstract void destroyHolder(BlockState state);

		/**
		 * IDK what this does, but it's here
		 *
		 * @return true if something was done
		 */
		public abstract boolean ambiguousA(Block block) throws WorldEditorException;

	}

	private class RevertEdit extends Edit {

		public RevertEdit(long time, Location loc, String playerName, int replaced, int type, byte data, String signtext, SerializableItemStack itemStack) {
			super(time, loc, playerName, replaced, type, data, signtext, itemStack);
		}

		@Override
		public boolean checkBlackList() {
			return !dontRollback.contains(beforeID);
		}

		@Override
		public boolean checkAction(Block block) {
			return !(beforeID == 0 && block.getTypeId() == 0);
		}

		@Override
		public PerformResult performChest(Block block, BlockState state) throws WorldEditorException {
			if (afterID == 0) {
				if (!block.setTypeId(0)) throw new WorldEditorException(block.getTypeId(), 0, block.getLocation());
			} else if (itemStack != null && (afterID == 23 || afterID == 54 || afterID == 61 || afterID == 62)) {
				int leftover;

				try {
					leftover = BukkitUtils.revertContainer(state, itemStack);
					if (leftover > 0)
						for (final BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST})
							if (block.getRelative(face).getTypeId() == 54) {
								ItemStack k = itemStack.toBukkit();
								k.setAmount(leftover);
								leftover = BukkitUtils.revertContainer(block.getRelative(face).getState(), SerializableItemStackFactory.makeItemStack(k, itemStack.wasAdded()));
							}
				} catch (final Exception ex) {
					throw new WorldEditorException(ex.getMessage(), block.getLocation());
				}
				if (!state.update())
					throw new WorldEditorException("Failed to update inventory of " + materialName(block.getTypeId()), block.getLocation());
				// TODO was && result.getAmount() < 0 (is this needed?)
				if (leftover > 0)
					throw new WorldEditorException("Not enough space left in " + materialName(block.getTypeId()), block.getLocation());
			} else
				return PerformResult.NO_ACTION;
			return PerformResult.SUCCESS;
		}

		@Override
		public boolean checkEqual(Block block) {
			return equalTypes(block.getTypeId(), afterID) || replaceAnyway.contains(block.getTypeId());
		}

		@Override
		public void destroyHolder(BlockState state) {
			if (state instanceof InventoryHolder) {
				((InventoryHolder)state).getInventory().clear();
				state.update();
			}
		}

		@Override
		public boolean ambiguousA(Block block) throws WorldEditorException {
			if (block.getTypeId() == beforeID) {
				if (block.getData() != (afterID == 0 ? data : (byte) 0)) {
					block.setData(afterID == 0 ? data : (byte) 0, true);
				} else {
					return false;
				}
			} else if (!block.setTypeIdAndData(beforeID, afterID == 0 ? data : (byte) 0, true))
				throw new WorldEditorException(block.getTypeId(), beforeID, block.getLocation());
			return true;
		}
	}

	private class RestoreEdit extends Edit {

		public RestoreEdit(long time, Location loc, String playerName, int replaced, int type, byte data, String signtext, SerializableItemStack itemStack) {
			super(time, loc, playerName, replaced, type, data, signtext, itemStack);
		}

		@Override
		public boolean checkBlackList() {
			return !dontRollback.contains(beforeID);
		}

		@Override
		public boolean checkAction(Block block) {
			return !(beforeID == 0 && block.getTypeId() == 0);
		}

		@Override
		public PerformResult performChest(Block block, BlockState state) throws WorldEditorException {
			if (afterID == 0) {
				if (!block.setTypeId(0)) throw new WorldEditorException(block.getTypeId(), 0, block.getLocation());
			} else if (itemStack != null && (afterID == 23 || afterID == 54 || afterID == 61 || afterID == 62)) {
				int leftover;

				try {
					leftover = BukkitUtils.restoreContainer(state, itemStack);
					if (leftover > 0)
						for (final BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST})
							if (block.getRelative(face).getTypeId() == 54) {
								ItemStack k = itemStack.toBukkit();
								k.setAmount(leftover);
								leftover = BukkitUtils.restoreContainer(block.getRelative(face).getState(), SerializableItemStackFactory.makeItemStack(k, itemStack.wasAdded()));
							}
				} catch (final Exception ex) {
					throw new WorldEditorException(ex.getMessage(), block.getLocation());
				}
				if (!state.update())
					throw new WorldEditorException("Failed to update inventory of " + materialName(block.getTypeId()), block.getLocation());
				// TODO was && result.getAmount() < 0 (is this needed?)
				if (leftover > 0)
					throw new WorldEditorException("Not enough space left in " + materialName(block.getTypeId()), block.getLocation());
			} else
				return PerformResult.NO_ACTION;
			return PerformResult.SUCCESS;
		}

		@Override
		public boolean checkEqual(Block block) {
			return equalTypes(block.getTypeId(), afterID) || replaceAnyway.contains(block.getTypeId());
		}

		@Override
		public void destroyHolder(BlockState state) {
			if (state instanceof InventoryHolder) {
				((InventoryHolder)state).getInventory().clear();
				state.update();
			}
		}

		@Override
		public boolean ambiguousA(Block block) throws WorldEditorException {
			if (block.getTypeId() == beforeID) {
				if (block.getData() != (afterID == 0 ? data : (byte) 0)) {
					block.setData(afterID == 0 ? data : (byte) 0, true);
				} else {
					return false;
				}
			} else if (!block.setTypeIdAndData(beforeID, afterID == 0 ? data : (byte) 0, true))
				throw new WorldEditorException(block.getTypeId(), beforeID, block.getLocation());
			return true;
		}
	}

	@SuppressWarnings("serial")
	public static class WorldEditorException extends Exception implements LookupCacheElement
	{
		private final Location loc;

		public WorldEditorException(int typeBefore, int typeAfter, Location loc) {
			this("Failed to replace " + materialName(typeBefore) + " with " + materialName(typeAfter), loc);
		}

		public WorldEditorException(String msg, Location loc) {
			super(msg + " at " + loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ());
			this.loc = loc;
		}

		@Override
		public Location getLocation() {
			return loc;
		}
	}
}
