package de.diddiz.LogBlock.platform.paper;

import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.platform.TestPlatformAdapter;

public class PaperPlatformAdapter extends TestPlatformAdapter {
    public PaperPlatformAdapter(LogBlock plugin) {
        if (Boolean.getBoolean("logblock.test.adapter.fail")) {
            throw new IllegalStateException("Test adapter failure");
        }
    }
}
