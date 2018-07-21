package de.diddiz.worldedit;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import de.diddiz.LogBlock.config.Config;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;

import java.util.logging.Level;

//...so they ALSO have a class called Actor... need to fully-qualify when we use ours

public class WorldEditLoggingHook {

    private LogBlock plugin;

    public WorldEditLoggingHook(LogBlock plugin) {
        this.plugin = plugin;
    }

    // Convert WE Actor to LB Actor
    private de.diddiz.LogBlock.Actor AtoA(Actor weActor) {
        if (weActor.isPlayer()) {
            return new de.diddiz.LogBlock.Actor(weActor.getName(), weActor.getUniqueId());
        }
        return new de.diddiz.LogBlock.Actor(weActor.getName());
    }

    private World adapt(com.sk89q.worldedit.world.World weWorld) {
        if (weWorld == null) {
            throw new NullPointerException("[Logblock-Worldedit] The provided world was null.");
        }
        if (weWorld instanceof BukkitWorld) {
            return ((BukkitWorld) weWorld).getWorld();
        }
        World world = Bukkit.getServer().getWorld(weWorld.getName());
        if (world == null) {
            throw new IllegalArgumentException("Can't find a Bukkit world for " + weWorld);
        }
        return world;
    }

    public void hook() {
        WorldEdit.getInstance().getEventBus().register(new Object() {
            @Subscribe
            public void wrapForLogging(final EditSessionEvent event) {
                final Actor actor = event.getActor();
                if (actor == null) {
                    return;
                }
                final de.diddiz.LogBlock.Actor lbActor = AtoA(actor);

                // Check to ensure the world should be logged
                final World world;
                final com.sk89q.worldedit.world.World k = event.getWorld();
                try {
                    world = adapt(k);
                } catch (RuntimeException ex) {
                    plugin.getLogger().warning("Failed to register logging for WorldEdit!");
                    plugin.getLogger().log(Level.WARNING, ex.getMessage(), ex);
                    return;
                }

                // If config becomes reloadable, this check should be moved
                if (!(Config.isLogging(world, Logging.WORLDEDIT))) {
                    return;
                }

                event.setExtent(new AbstractDelegateExtent(event.getExtent()) {
                    @Override
                    public final boolean setBlock(Vector position, @SuppressWarnings("rawtypes") BlockStateHolder block) throws WorldEditException {
                        onBlockChange(position, block);
                        return super.setBlock(position, block);
                    }
                    
                    protected void onBlockChange(Vector pt, BlockStateHolder<?> block) {

                        if (event.getStage() != EditSession.Stage.BEFORE_CHANGE) {
                            return;
                        }

                        Location location = new Location(world, pt.getBlockX(), pt.getBlockY(), pt.getBlockZ());
                        Block origin = location.getBlock();
                        Material typeBefore = origin.getType();

                        // Check to see if we've broken a sign
                        if (Config.isLogging(location.getWorld().getName(), Logging.SIGNTEXT) && (typeBefore == Material.SIGN || typeBefore == Material.WALL_SIGN)) {
                            BlockState stateBefore = origin.getState();
                            plugin.getConsumer().queueSignBreak(lbActor, (Sign) stateBefore);
                        } else if (!origin.isEmpty()) {
                            plugin.getConsumer().queueBlockBreak(lbActor, location, origin.getBlockData());
                        }
                        BlockData newBlock = BukkitAdapter.adapt(block);
                        if (newBlock != null && newBlock.getMaterial() != Material.AIR) {
                            plugin.getConsumer().queueBlockPlace(lbActor, location, newBlock);
                        }
                    }
                });
            }
        });
    }
}
