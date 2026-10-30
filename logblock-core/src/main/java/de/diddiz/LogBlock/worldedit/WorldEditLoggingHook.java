package de.diddiz.LogBlock.worldedit;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import de.diddiz.LogBlock.config.Config;
import de.diddiz.LogBlock.util.BukkitUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;

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

    public void hook() {
        WorldEdit.getInstance().getEventBus().register(new Object() {
            @Subscribe
            public void wrapForLogging(final EditSessionEvent event) {
                if (event.getStage() != EditSession.Stage.BEFORE_CHANGE) {
                    return;
                }
                final Actor actor = event.getActor();
                if (actor == null) {
                    return;
                }
                final de.diddiz.LogBlock.Actor lbActor = AtoA(actor);

                // Check to ensure the world should be logged
                final World world;
                try {
                    world = BukkitAdapter.adapt(event.getWorld());
                } catch (RuntimeException ex) {
                    plugin.getLogger().warning("Failed to register logging for WorldEdit!");
                    plugin.getLogger().log(Level.WARNING, ex.getMessage(), ex);
                    return;
                }

                // If config becomes reloadable, this check should be moved
                if (!(Config.isLogging(world, Logging.WORLDEDIT))) {
                    return;
                }

                event.setExtent(loggingExtent(event.getExtent(), world, (before, after) -> {
                    if (!BukkitUtils.isEmpty(before.getType())) {
                        plugin.getConsumer().queueBlockBreak(lbActor, before);
                    }
                    if (!BukkitUtils.isEmpty(after.getType())) {
                        plugin.getConsumer().queueBlockPlace(lbActor, after);
                    }
                }));
            }
        });
    }

    static Extent loggingExtent(Extent extent, World world, BiConsumer<BlockState, BlockState> logger) {
        return new AbstractDelegateExtent(extent) {
            private final Map<String, BlockData> blockData = new HashMap<>();

            @Override
            public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 position, B block) throws WorldEditException {
                Location location = new Location(world, position.x(), position.y(), position.z());
                BlockState before = location.getBlock().getState();
                BlockData requested = blockData.computeIfAbsent(block.getAsString(), Bukkit::createBlockData);
                boolean logChange = !before.getBlockData().equals(requested);
                boolean changed = super.setBlock(position, block);
                if (changed && logChange) {
                    logger.accept(before, location.getBlock().getState());
                }
                return changed;
            }
        };
    }
}
