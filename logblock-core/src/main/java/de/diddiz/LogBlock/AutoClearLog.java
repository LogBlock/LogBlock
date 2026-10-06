package de.diddiz.LogBlock;

import java.util.Arrays;
import java.util.logging.Level;

import static de.diddiz.LogBlock.config.Config.autoClearLog;
import static org.bukkit.Bukkit.*;

public class AutoClearLog implements Runnable {
    private final LogBlock logblock;

    AutoClearLog(LogBlock logblock) {
        this.logblock = logblock;
    }

    @Override
    public void run() {
        final CommandsHandler handler = logblock.getCommandsHandler();
        for (final String paramStr : autoClearLog) {
            if (!logblock.isCompletelyEnabled()) {
                return; // do not try when plugin is disabled
            }
            try {
                final QueryParams params = new QueryParams(logblock, getConsoleSender(), Arrays.asList(paramStr.split(" ")));
                params.noForcedLimit = true;
                handler.new CommandClearLog(getServer().getConsoleSender(), params, false);
            } catch (final Exception ex) {
                getLogger().log(Level.SEVERE, "Failed to schedule auto ClearLog: ", ex);
            }
        }
    }
}
