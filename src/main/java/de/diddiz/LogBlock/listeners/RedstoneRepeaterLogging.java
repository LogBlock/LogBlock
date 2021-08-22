package de.diddiz.LogBlock.listeners;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import de.diddiz.LogBlock.config.WorldConfig;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;

import static de.diddiz.LogBlock.config.Config.getWorldConfig;

public class RedstoneRepeaterLogging extends LoggingListener {
    private final LogBlock logBlock;

    public RedstoneRepeaterLogging(LogBlock logBlock) {
        super(logBlock);
        this.logBlock = logBlock;
    }

    @EventHandler
    public void onRepeaterChange(PlayerInteractEvent event) {
        final WorldConfig wcfg = getWorldConfig(event.getPlayer().getWorld());

        final Player player = event.getPlayer();
        final Block clickedBlock = event.getClickedBlock();
        if (wcfg == null || !wcfg.isLogging(Logging.REPEATER)) {
            return;
        }
        if (clickedBlock == null) {
            return;
        }
        if (clickedBlock.getType() != Material.REPEATER) {
            return;
        }

        final Location location = clickedBlock.getLocation();

        final BlockData oldBlockData = clickedBlock.getBlockData().clone();
        // Run task 1 tick later so we can retrieve new BlockData
        logBlock.getServer().getScheduler().runTaskLater(logBlock, () -> {
            final World world = location.getWorld();
            if (world == null) {
                return;
            }

            final Block newBlock = world.getBlockAt(clickedBlock.getLocation());
            final BlockData newBlockData = newBlock.getBlockData();

            if(!newBlockData.equals(oldBlockData)) {
                consumer.queueBlock(Actor.actorFromEntity(player), location, oldBlockData, newBlockData);
            }
        }, 1);
    }
}
