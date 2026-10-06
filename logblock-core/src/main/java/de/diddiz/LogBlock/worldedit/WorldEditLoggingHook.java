package de.diddiz.LogBlock.worldedit;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import de.diddiz.LogBlock.blockstate.BlockStateCodecs;
import de.diddiz.LogBlock.config.Config;
import de.diddiz.LogBlock.util.BukkitUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
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

                event.setExtent(new AbstractDelegateExtent(event.getExtent()) {
                    @Override
                    public final <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 position, B block) throws WorldEditException {
                        onBlockChange(position, block);
                        return super.setBlock(position, block);
                    }

                    protected <B extends BlockStateHolder<B>> void onBlockChange(BlockVector3 pt, B block) {

                        if (event.getStage() != EditSession.Stage.BEFORE_CHANGE) {
                            return;
                        }

                        Location location = BukkitAdapter.adapt(world, pt);
                        Block blockBefore = location.getBlock();
                        BlockData blockDataBefore = blockBefore.getBlockData();
                        Material typeBefore = blockDataBefore.getMaterial();

                        BlockData blockDataNew = BukkitAdapter.adapt(block);

                        if (!blockDataBefore.equals(blockDataNew)) {
                            // Check to see if we've broken a sign
                            if (BlockStateCodecs.hasCodec(typeBefore)) {
                                plugin.getConsumer().queueBlockBreak(lbActor, blockBefore.getState());
                            } else if (!BukkitUtils.isEmpty(typeBefore)) {
                                plugin.getConsumer().queueBlockBreak(lbActor, location, blockDataBefore);
                            }
                            if (!BukkitUtils.isEmpty(blockDataNew.getMaterial())) {
                                plugin.getConsumer().queueBlockPlace(lbActor, location, blockDataNew);
                            }
                        }
                    }
                });
            }
        });
    }
}
