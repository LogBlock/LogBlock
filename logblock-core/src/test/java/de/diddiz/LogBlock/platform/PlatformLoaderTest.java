package de.diddiz.LogBlock.platform;

import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class PlatformLoaderTest {
    private static class ServerLoader extends ClassLoader {
        final boolean paper;
        final List<String> adapters = new ArrayList<>();

        ServerLoader(boolean paper) { super(PlatformLoaderTest.class.getClassLoader()); this.paper = paper; }

        @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.equals(PlatformLoader.PAPER_MARKER) && !paper) throw new ClassNotFoundException(name);
            if (name.equals(PlatformLoader.PAPER_ADAPTER) || name.equals(PlatformLoader.SPIGOT_ADAPTER)) adapters.add(name);
            return super.loadClass(name, resolve);
        }
    }

    @After public void clearProperties() {
        System.clearProperty("logblock.test.adapter.fail");
        System.clearProperty("logblock.test.marker.initialized");
    }

    @Test public void spigotLoadsOnlySpigot() throws Exception {
        ServerLoader loader = new ServerLoader(false);
        assertEquals(PlatformLoader.SPIGOT_ADAPTER, PlatformLoader.load(null, loader).getClass().getName());
        assertEquals(List.of(PlatformLoader.SPIGOT_ADAPTER), loader.adapters);
    }

    @Test public void paperLoadsOnlyPaperAndDoesNotInitializeMarker() throws Exception {
        ServerLoader loader = new ServerLoader(true);
        assertEquals(PlatformLoader.PAPER_ADAPTER, PlatformLoader.load(null, loader).getClass().getName());
        assertEquals(List.of(PlatformLoader.PAPER_ADAPTER), loader.adapters);
        assertNull(System.getProperty("logblock.test.marker.initialized"));
    }

    @Test public void paperInitializationFailureDoesNotFallBack() {
        ServerLoader loader = new ServerLoader(true);
        System.setProperty("logblock.test.adapter.fail", "true");
        var failure = assertThrows(ReflectiveOperationException.class, () -> PlatformLoader.load(null, loader));
        assertTrue(failure.getCause() instanceof IllegalStateException);
        assertEquals(List.of(PlatformLoader.PAPER_ADAPTER), loader.adapters);
    }

    @Test public void brokenPaperApiDoesNotFallBack() {
        ServerLoader loader = new ServerLoader(true) {
            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.equals(PlatformLoader.PAPER_MARKER)) throw new NoClassDefFoundError("Broken Paper API");
                return super.loadClass(name, resolve);
            }
        };
        assertThrows(NoClassDefFoundError.class, () -> PlatformLoader.load(null, loader));
        assertTrue(loader.adapters.isEmpty());
    }
}
