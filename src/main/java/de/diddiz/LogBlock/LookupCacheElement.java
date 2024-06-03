package de.diddiz.LogBlock;

import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Location;

public interface LookupCacheElement {
    public Location getLocation();

    public default BaseComponent getLogMessage() {
        return getLogMessage(-1);
    }

    public BaseComponent getLogMessage(int entry);

    public default int getNumChanges() {
        return 1;
    }
}
