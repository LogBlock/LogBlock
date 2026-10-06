package de.diddiz.LogBlock.platform;

import de.diddiz.LogBlock.LogBlock;
import java.lang.reflect.InvocationTargetException;

/** Loads only the selected adapter, without linking Core to either implementation. */
public final class PlatformLoader {
    static final String PAPER_MARKER = "io.papermc.paper.ServerBuildInfo";
    static final String PAPER_ADAPTER = "de.diddiz.LogBlock.platform.paper.PaperPlatformAdapter";
    static final String SPIGOT_ADAPTER = "de.diddiz.LogBlock.platform.spigot.SpigotPlatformAdapter";

    private PlatformLoader() {
    }

    static String selectAdapter(ClassLoader loader) {
        try {
            Class.forName(PAPER_MARKER, false, loader);
            return PAPER_ADAPTER;
        } catch (ClassNotFoundException absent) {
            return SPIGOT_ADAPTER;
        }
    }

    public static PlatformAdapter load(LogBlock plugin, ClassLoader loader) throws ReflectiveOperationException {
        String adapter = selectAdapter(loader);
        try {
            return Class.forName(adapter, true, loader).asSubclass(PlatformAdapter.class)
                    .getConstructor(LogBlock.class).newInstance(plugin);
        } catch (InvocationTargetException failure) {
            throw new ReflectiveOperationException("Could not initialize " + adapter, failure.getCause());
        }
    }
}
