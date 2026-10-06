package de.diddiz.LogBlock.util;

import org.bukkit.Material;

/** Materials whose vertical segments share the existing dripstone logging rules. */
public final class SpeleothemUtils {
    private SpeleothemUtils() {
    }

    public static boolean isSpeleothem(Material material) {
        return material == Material.POINTED_DRIPSTONE || material == Material.SULFUR_SPIKE;
    }
}
