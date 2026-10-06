package de.diddiz.LogBlock.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.BlockVector;

public class CuboidRegion implements Cloneable {

    private World world;
    private BlockVector min = new BlockVector();
    private BlockVector max = new BlockVector();

    public CuboidRegion(World world, BlockVector first, BlockVector second) {
        this.world = world;
        this.min.setX(Math.min(first.getBlockX(), second.getBlockX()));
        this.min.setY(Math.min(first.getBlockY(), second.getBlockY()));
        this.min.setZ(Math.min(first.getBlockZ(), second.getBlockZ()));
        this.max.setX(Math.max(first.getBlockX(), second.getBlockX()));
        this.max.setY(Math.max(first.getBlockY(), second.getBlockY()));
        this.max.setZ(Math.max(first.getBlockZ(), second.getBlockZ()));
    }

    public static CuboidRegion fromCorners(World world, Location first, Location second) {
        return new CuboidRegion(world, new BlockVector(first.getBlockX(), first.getBlockY(), first.getBlockZ()), new BlockVector(second.getBlockX(), second.getBlockY(), second.getBlockZ()));
    }

    public World getWorld() {
        return world;
    }

    public BlockVector getMinimumPoint() {
        return min;
    }

    public BlockVector getMaximumPoint() {
        return max;
    }

    public int getSizeX() {
        return max.getBlockX() - min.getBlockX() + 1;
    }

    public int getSizeZ() {
        return max.getBlockZ() - min.getBlockZ() + 1;
    }

    @Override
    public CuboidRegion clone() {
        try {
            CuboidRegion clone = (CuboidRegion) super.clone();
            clone.min = min.clone();
            clone.max = max.clone();
            return clone;
        } catch (final CloneNotSupportedException ex) {
            throw new Error("CuboidRegion should be cloneable", ex);
        }
    }
}
