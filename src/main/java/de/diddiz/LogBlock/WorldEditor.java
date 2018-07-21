package de.diddiz.LogBlock;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Bed.Part;
import org.bukkit.block.data.type.Piston;
import org.bukkit.block.data.type.PistonHead;
import org.bukkit.block.data.type.TechnicalPiston.Type;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

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
import static de.diddiz.util.BukkitUtils.*;
import static org.bukkit.Bukkit.getLogger;

public class WorldEditor implements Runnable {
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

    public void queueEdit(int x, int y, int z, int replaced, int replaceData, int type, int typeData, String signtext, ChestAccess item) {
        edits.add(new Edit(0, new Location(world, x, y, z), null, replaced, replaceData, type, typeData, signtext, item));
    }

    public long getElapsedTime() {
        return elapsedTime;
    }

    synchronized public void start() throws Exception {
        final long start = System.currentTimeMillis();
        taskID = logblock.getServer().getScheduler().scheduleSyncRepeatingTask(logblock, this, 0, 1);
        if (taskID == -1) {
            throw new Exception("Failed to schedule task");
        }
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
                    case NO_ACTION:
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
            if (errorList.size() > 0) {
                try {
                    final File file = new File("plugins/LogBlock/error/WorldEditor-" + new SimpleDateFormat("yy-MM-dd-HH-mm-ss").format(System.currentTimeMillis()) + ".log");
                    file.getParentFile().mkdirs();
                    final PrintWriter writer = new PrintWriter(file);
                    for (final LookupCacheElement err : errorList) {
                        writer.println(err.getMessage());
                    }
                    writer.close();
                } catch (final Exception ex) {
                }
            }
            errors = errorList.toArray(new WorldEditorException[errorList.size()]);
            notify();
        }
    }

    private static enum PerformResult {
        SUCCESS, BLACKLISTED, NO_ACTION
    }

    private class Edit extends BlockChange {
        public Edit(long time, Location loc, Actor actor, int replaced, int replaceData, int type, int typeData, String signtext, ChestAccess ca) {
            super(time, loc, actor, replaced, replaceData, type, typeData, signtext, ca);
        }

        PerformResult perform() throws WorldEditorException {
            BlockData replacedBlock = MaterialConverter.getBlockData(this.replacedMaterial, replacedData);
            BlockData setBlock = MaterialConverter.getBlockData(this.typeMaterial, typeData);
            // action: set to replaced
            
            if (dontRollback.contains(replacedBlock.getMaterial())) {
                return PerformResult.BLACKLISTED;
            }
            final Block block = loc.getBlock();
            if (replacedBlock.getMaterial() == Material.AIR && block.getType() == Material.AIR) {
                return PerformResult.NO_ACTION;
            }
            final BlockState state = block.getState();
            if (!world.isChunkLoaded(block.getChunk())) {
                world.loadChunk(block.getChunk());
            }
            if (setBlock.equals(replacedBlock)) {
                if (setBlock.getMaterial() == Material.AIR) {
                    block.setType(Material.AIR);
                } else if (ca != null) {
                    if (state instanceof InventoryHolder) {
                        int leftover;
                        try {
                            leftover = modifyContainer(state, new ItemStack(ca.itemStack), !ca.remove);
                            // Special-case blocks which might be double chests
                            if (leftover > 0 && (setBlock.getMaterial() == Material.CHEST || setBlock.getMaterial() == Material.TRAPPED_CHEST)) {
                                for (final BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
                                    if (block.getRelative(face).getType() == setBlock.getMaterial()) {
                                        ItemStack remaining = new ItemStack(ca.itemStack);
                                        remaining.setAmount(leftover);
                                        leftover = modifyContainer(block.getRelative(face).getState(), remaining, !ca.remove);
                                    }
                                }
                            }
                        } catch (final Exception ex) {
                            throw new WorldEditorException(ex.getMessage(), block.getLocation());
                        }
                        if (leftover > 0 && ca.remove) {
                            throw new WorldEditorException("Not enough space left in " + block.getType(), block.getLocation());
                        }
                    }
                } else {
                    return PerformResult.NO_ACTION;
                }
                return PerformResult.SUCCESS;
            }
            if (block.getType() != setBlock.getMaterial() && !replaceAnyway.contains(block.getType())) {
                return PerformResult.NO_ACTION;
            }
            if (state instanceof InventoryHolder) {
                ((InventoryHolder) state).getInventory().clear();
                state.update();
            }
            block.setBlockData(replacedBlock);
            BlockData newData = block.getBlockData();
            
            final Material curtype = block.getType();
            if (signtext != null && (curtype == Material.SIGN || curtype == Material.WALL_SIGN)) {
                final Sign sign = (Sign) block.getState();
                final String[] lines = signtext.split("\0", 4);
                if (lines.length < 4) {
                    return PerformResult.NO_ACTION;
                }
                for (int i = 0; i < 4; i++) {
                    sign.setLine(i, lines[i]);
                }
                if (!sign.update()) {
                    throw new WorldEditorException("Failed to update signtext of " + block.getType(), block.getLocation());
                }
            } else if (newData instanceof Bed) {
                final Bed bed = (Bed) newData;
                final Block secBlock = bed.getPart() == Part.HEAD ? block.getRelative(bed.getFacing().getOppositeFace()) : block.getRelative(bed.getFacing());
                if (secBlock.isEmpty()) {
                    Bed bed2 = (Bed) bed.clone();
                    bed2.setPart(bed.getPart() == Part.HEAD ? Part.FOOT : Part.HEAD);
                    secBlock.setBlockData(bed2);
                }
            } else if ((curtype == Material.PISTON || curtype == Material.STICKY_PISTON)) {
                Piston piston = (Piston) newData;
                if (piston.isExtended()) {
                    final Block secBlock = block.getRelative(piston.getFacing());
                    if (secBlock.isEmpty()) {
                        PistonHead head = (PistonHead) Material.PISTON_HEAD.createBlockData();
                        head.setFacing(piston.getFacing());
                        head.setType(curtype == Material.PISTON ? Type.NORMAL : Type.STICKY);
                        secBlock.setBlockData(head);
                    }
                }
            } else if (curtype == Material.PISTON_HEAD) {
                PistonHead head = (PistonHead) newData;
                final Block secBlock = block.getRelative(head.getFacing().getOppositeFace());
                if (secBlock.isEmpty()) {
                    Piston piston = (Piston) (head.getType() == Type.NORMAL ? Material.PISTON : Material.STICKY_PISTON).createBlockData();
                    piston.setFacing(head.getFacing());
                    piston.setExtended(true);
                    secBlock.setBlockData(piston);
                }
            }
            return PerformResult.SUCCESS;
        }
    }

    @SuppressWarnings("serial")
    public static class WorldEditorException extends Exception implements LookupCacheElement {
        private final Location loc;

        public WorldEditorException(Material typeBefore, Material typeAfter, Location loc) {
            this("Failed to replace " + typeBefore.name() + " with " + typeAfter.name(), loc);
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
