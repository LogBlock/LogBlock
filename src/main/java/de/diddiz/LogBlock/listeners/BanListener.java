package de.diddiz.LogBlock.listeners;

import de.diddiz.LogBlock.CommandsHandler;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.QueryParams;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import static de.diddiz.LogBlock.config.Config.banPermission;
import static de.diddiz.LogBlock.config.Config.isLogged;
import static org.bukkit.Bukkit.getScheduler;

public class BanListener implements Listener {
    private final CommandsHandler handler;
    private final LogBlock logblock;

    public BanListener(LogBlock logblock) {
        this.logblock = logblock;
        handler = logblock.getCommandsHandler();
    }

    @EventHandler
    public void onPlayerCommandPreprocess(final PlayerCommandPreprocessEvent event) {
        final String[] split = event.getMessage().split(" ");
        if (split.length > 1 && split[0].equalsIgnoreCase("/ban") && logblock.hasPermission(event.getPlayer(), banPermission)) {
            final QueryParams p = new QueryParams(logblock);
            p.setPlayer(split[1].equalsIgnoreCase("g") ? split[2] : split[1]);
            p.since = 0;
            p.silent = false;
            getScheduler().runTaskAsynchronously(logblock, new Runnable() {
                @Override
                public void run() {
                    for (final World world : logblock.getServer().getWorlds()) {
                        if (isLogged(world)) {
                            p.world = world;
                            try {
                                handler.new CommandRollback(event.getPlayer(), p, false);
                            } catch (final Exception ex) {
                            }
                        }
                    }
                }
            });
        }
    }
}
