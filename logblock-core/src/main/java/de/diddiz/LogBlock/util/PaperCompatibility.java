package de.diddiz.LogBlock.util;

import de.diddiz.LogBlock.LogBlock;
import org.bukkit.block.data.type.Shelf;

/** Compatibility facade for callers of the original helper. */
@Deprecated
public class PaperCompatibility {
    public enum SideChain {
        LEFT, CENTER, RIGHT, UNCONNECTED
    }

    public static SideChain getShelfSideChain(Shelf shelf) {
        return SideChain.valueOf(LogBlock.getInstance().getPlatformAdapter().getShelfSideChain(shelf).name());
    }
}
