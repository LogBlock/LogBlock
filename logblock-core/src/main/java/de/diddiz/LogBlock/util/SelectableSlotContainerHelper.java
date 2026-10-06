package de.diddiz.LogBlock.util;

import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

public class SelectableSlotContainerHelper {
    public static int getHitSlot(BlockFace face, Vector clickPosition, int rows, int columns) {
        double clickX = switch (face) {
            case NORTH -> 1 - clickPosition.getX();
            case SOUTH -> clickPosition.getX();
            case EAST -> 1 - clickPosition.getZ();
            case WEST -> clickPosition.getZ();
            default -> throw new IllegalArgumentException("Unexpected facing for SelectableSlotContainer: " + face);
        };
        double clickY = clickPosition.getY();
        int sectionY = getSection(1.0F - clickY, rows);
        int sectionX = getSection(clickX, columns);
        return sectionX + sectionY * columns;
    }

    private static int getSection(double x, int slots) {
        return Math.clamp((int) Math.floor(x * slots), 0, slots - 1);
    }
}
