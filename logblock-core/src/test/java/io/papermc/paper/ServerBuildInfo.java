package io.papermc.paper;

/** Test marker: selecting Paper must not initialize its API. */
public final class ServerBuildInfo {
    static {
        System.setProperty("logblock.test.marker.initialized", "true");
    }
}
