package de.diddiz.LogBlock;

import de.diddiz.LogBlock.componentwrapper.Component;
import org.bukkit.Location;

public interface LookupCacheElement {
    public Location getLocation();

    public default Component getLogMessage() {
        return getLogMessage(-1);
    }

    public Component getLogMessage(int entry);

    public default int getNumChanges() {
        return 1;
    }
}
