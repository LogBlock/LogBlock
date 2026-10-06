package de.diddiz.LogBlock.listeners;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import java.util.ArrayDeque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import static de.diddiz.LogBlock.config.Config.isLogging;
import static de.diddiz.LogBlock.util.LoggingUtil.smartLogFallables;

public class ScaffoldingLogging extends LoggingListener {
    private final static long MAX_SCAFFOLDING_LOG_TIME_MS = 2000;
    private final static EnumSet<BlockFace> NEIGHBOURS_SIDES_AND_UP = EnumSet.of(BlockFace.UP, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST);
    private final static EnumSet<BlockFace> NEIGHBOURS_SIDES_AND_BELOW = EnumSet.of(BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST);

    private final ArrayDeque<ScaffoldingBreaker> scaffoldingBreakersList = new ArrayDeque<>();
    private final HashMap<Location, ScaffoldingBreaker> scaffoldingBreakersByLocation = new HashMap<>();
    private final HashMap<Location, Actor> scaffoldingPlacersByLocation = new HashMap<>();

    public ScaffoldingLogging(LogBlock lb) {
        super(lb);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event) {
        Block block = event.getBlock();
        if (isLogging(block.getWorld(), Logging.SCAFFOLDING)) {
            final Material type = block.getType();
            if (type == Material.SCAFFOLDING) {
                Actor actor = scaffoldingPlacersByLocation.get(block.getLocation()); // get placer before cleanupScaffoldingBreakers
                cleanupScaffoldingBreakers();
                if (actor == null) {
                    actor = getScaffoldingBreaker(block);
                    if (actor != null) {
                        for (BlockFace dir : NEIGHBOURS_SIDES_AND_UP) {
                            Block otherBlock = block.getRelative(dir);
                            if (otherBlock.getType() == Material.SCAFFOLDING) {
                                addScaffoldingBreaker(actor, otherBlock);
                            }
                        }
                    } else {
                        actor = new Actor("ScaffoldingFall");
                    }
                }
                consumer.queueBlockReplace(actor, block.getState(), event.getNewState());
                smartLogFallables(consumer, actor, block);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (isLogging(block.getWorld(), Logging.SCAFFOLDING)) {
            cleanupScaffoldingBreakers();
            Block otherBlock;
            if (block.getType() == Material.SCAFFOLDING) {
                for (BlockFace dir : NEIGHBOURS_SIDES_AND_UP) {
                    otherBlock = block.getRelative(dir);
                    if (otherBlock.getType() == Material.SCAFFOLDING) {
                        addScaffoldingBreaker(Actor.actorFromEntity(event.getPlayer()), otherBlock);
                    }
                }
            } else if ((otherBlock = block.getRelative(BlockFace.UP)).getType() == Material.SCAFFOLDING) {
                addScaffoldingBreaker(Actor.actorFromEntity(event.getPlayer()), otherBlock);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (isLogging(block.getWorld(), Logging.SCAFFOLDING)) {
            cleanupScaffoldingBreakers();
            if (block.getType() == Material.SCAFFOLDING) {
                scaffoldingPlacersByLocation.put(block.getLocation(), Actor.actorFromEntity(event.getPlayer()));
            }
        }
    }

    public void addScaffoldingBreaker(Actor actor, Block block) {
        ScaffoldingBreaker breaker = new ScaffoldingBreaker(actor, block.getLocation());
        scaffoldingBreakersList.addLast(breaker);
        scaffoldingBreakersByLocation.put(breaker.getLocation(), breaker);

    }

    private void cleanupScaffoldingBreakers() {
        if (!scaffoldingPlacersByLocation.isEmpty()) {
            scaffoldingPlacersByLocation.clear();
        }
        if (!scaffoldingBreakersList.isEmpty()) {
            long time = System.currentTimeMillis() - MAX_SCAFFOLDING_LOG_TIME_MS;
            while (!scaffoldingBreakersList.isEmpty() && scaffoldingBreakersList.getFirst().getTime() < time) {
                ScaffoldingBreaker breaker = scaffoldingBreakersList.removeFirst();
                scaffoldingBreakersByLocation.remove(breaker.getLocation(), breaker);
            }
        }
    }

    private Actor getScaffoldingBreaker(Block block) {
        if (scaffoldingBreakersList.isEmpty()) {
            return null;
        }

        ScaffoldingBreaker breaker = scaffoldingBreakersByLocation.get(block.getLocation());
        if (breaker != null) {
            return breaker.getActor();
        }

        // Search all connected scaffoldings
        ArrayDeque<Block> front = new ArrayDeque<>();
        HashSet<Block> frontAndDone = new HashSet<>();
        front.addLast(block);
        frontAndDone.add(block);
        while (!front.isEmpty()) {
            Block current = front.removeFirst();
            Location loc = current.getLocation();

            breaker = scaffoldingBreakersByLocation.get(loc);
            if (breaker != null) {
                return breaker.getActor();
            }

            for (BlockFace dir : NEIGHBOURS_SIDES_AND_BELOW) {
                Block otherBlock = current.getRelative(dir);
                if (!frontAndDone.contains(otherBlock) && otherBlock.getType() == Material.SCAFFOLDING) {
                    front.addLast(otherBlock);
                    frontAndDone.add(otherBlock);
                }
            }
        }
        return null;
    }

    class ScaffoldingBreaker {
        protected final Actor actor;
        protected final long time;
        protected final Location location;

        public ScaffoldingBreaker(Actor actor, Location location) {
            this.actor = actor;
            this.location = location;
            this.time = System.currentTimeMillis();
        }

        public Actor getActor() {
            return actor;
        }

        public Location getLocation() {
            return location;
        }

        public long getTime() {
            return time;
        }
    }
}
