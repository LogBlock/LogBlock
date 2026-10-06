package de.diddiz.LogBlock.addons.worldguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.association.RegionAssociable;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.events.BlockChangePreLogEvent;
import de.diddiz.LogBlock.events.EntityChangePreLogEvent;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class WorldGuardLoggingFlagsAddon {
    private final StateFlag LOGBLOCK_LOG_BLOCKS = new StateFlag("logblock-log-blocks", true);
    private final StateFlag LOGBLOCK_LOG_ENTITIES = new StateFlag("logblock-log-entities", true);

    private LogBlock plugin;
    private WorldGuardPlugin worldGuard;

    public WorldGuardLoggingFlagsAddon(LogBlock plugin) {
        this.plugin = plugin;
    }

    public void onPluginLoad() {
        registerFlags();
    }

    public void onPluginEnable() {
        worldGuard = (WorldGuardPlugin) plugin.getServer().getPluginManager().getPlugin("WorldGuard");
        plugin.getServer().getPluginManager().registerEvents(new LoggingListener(), plugin);
    }

    private void registerFlags() {
        try {
            FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
            registry.register(LOGBLOCK_LOG_BLOCKS);
            registry.register(LOGBLOCK_LOG_ENTITIES);
        } catch (FlagConflictException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not initialize Flags", e);
        }
    }

    private class LoggingListener implements Listener {
        @EventHandler(ignoreCancelled = true)
        public void onBlockChangePreLog(BlockChangePreLogEvent event) {
            RegionAssociable regionAssociable = null;
            Location location = event.getLocation();
            Entity actorEntity = event.getOwnerActor().getEntity();
            if (actorEntity instanceof Player) {
                regionAssociable = worldGuard.wrapPlayer((Player) actorEntity);
            }
            ApplicableRegionSet set = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery().getApplicableRegions(BukkitAdapter.adapt(location));
            if (!set.testState(regionAssociable, LOGBLOCK_LOG_BLOCKS)) {
                event.setCancelled(true);
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void onEntityChangePreLog(EntityChangePreLogEvent event) {
            RegionAssociable regionAssociable = null;
            Location location = event.getLocation();
            Entity actorEntity = event.getOwnerActor().getEntity();
            if (actorEntity instanceof Player) {
                regionAssociable = worldGuard.wrapPlayer((Player) actorEntity);
            }
            ApplicableRegionSet set = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery().getApplicableRegions(BukkitAdapter.adapt(location));
            if (!set.testState(regionAssociable, LOGBLOCK_LOG_ENTITIES)) {
                event.setCancelled(true);
            }
        }
    }
}
