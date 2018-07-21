package de.diddiz.LogBlock.listeners;

import de.diddiz.LogBlock.*;
import de.diddiz.worldedit.RegionContainer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map.Entry;

import static de.diddiz.LogBlock.Session.getSession;
import static de.diddiz.LogBlock.Session.hasSession;
import static de.diddiz.LogBlock.config.Config.isLogged;
import static de.diddiz.LogBlock.config.Config.toolsByType;

public class ToolListener implements Listener {
    private final CommandsHandler handler;
    private final LogBlock logblock;

    public ToolListener(LogBlock logblock) {
        this.logblock = logblock;
        handler = logblock.getCommandsHandler();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getMaterial() != null) {
            final Action action = event.getAction();
            final Material type = event.getMaterial();
            final Tool tool = toolsByType.get(type);
            final Player player = event.getPlayer();
            if (tool != null && (action == Action.RIGHT_CLICK_BLOCK || action == Action.LEFT_CLICK_BLOCK) && logblock.hasPermission(player, "logblock.tools." + tool.name)) {
                final ToolBehavior behavior = action == Action.RIGHT_CLICK_BLOCK ? tool.rightClickBehavior : tool.leftClickBehavior;
                final ToolData toolData = getSession(player).toolData.get(tool);
                if (behavior != ToolBehavior.NONE && toolData.enabled) {
                    if (!isLogged(player.getWorld())) {
                        player.sendMessage(ChatColor.RED + "This world is not currently logged.");
                        event.setCancelled(true);
                        return;
                    }
                    final Block block = event.getClickedBlock();
                    final QueryParams params = toolData.params;
                    params.loc = null;
                    params.sel = null;
                    if (behavior == ToolBehavior.BLOCK) {
                        params.setLocation(block.getRelative(event.getBlockFace()).getLocation());
                    } else if ((block.getType() != Material.CHEST && block.getType() != Material.TRAPPED_CHEST) || tool.params.radius != 0) {
                        params.setLocation(block.getLocation());
                    } else {
                        if (logblock.getServer().getPluginManager().isPluginEnabled("WorldEdit")) {
                            for (final BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
                                if (block.getRelative(face).getType() == block.getType()) {
                                    params.setSelection(RegionContainer.fromCorners(event.getPlayer().getWorld(),
                                            block.getLocation(), block.getRelative(face).getLocation()));
                                }
                            }
                        }
                        if (params.sel == null) {
                            params.setLocation(block.getLocation());
                        }
                    }
                    try {
                        if (toolData.mode == ToolMode.ROLLBACK) {
                            handler.new CommandRollback(player, params, true);
                        } else if (toolData.mode == ToolMode.REDO) {
                            handler.new CommandRedo(player, params, true);
                        } else if (toolData.mode == ToolMode.CLEARLOG) {
                            handler.new CommandClearLog(player, params, true);
                        } else if (toolData.mode == ToolMode.WRITELOGFILE) {
                            handler.new CommandWriteLogFile(player, params, true);
                        } else {
                            handler.new CommandLookup(player, params, true);
                        }
                    } catch (final Exception ex) {
                        player.sendMessage(ChatColor.RED + ex.getMessage());
                    }
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        final Player player = event.getPlayer();
        if (hasSession(player)) {
            final Session session = getSession(player);
            for (final Entry<Tool, ToolData> entry : session.toolData.entrySet()) {
                final Tool tool = entry.getKey();
                final ToolData toolData = entry.getValue();
                if (toolData.enabled && !logblock.hasPermission(player, "logblock.tools." + tool.name)) {
                    toolData.enabled = false;
                    player.getInventory().removeItem(new ItemStack(tool.item, 1));
                    player.sendMessage(ChatColor.GREEN + "Tool disabled.");
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        final Player player = event.getPlayer();
        if (hasSession(player)) {
            final Session session = getSession(player);
            for (final Entry<Tool, ToolData> entry : session.toolData.entrySet()) {
                final Tool tool = entry.getKey();
                final ToolData toolData = entry.getValue();
                final Material item = event.getItemDrop().getItemStack().getType();
                if (item == tool.item && toolData.enabled && !tool.canDrop) {
                    player.sendMessage(ChatColor.RED + "You cannot drop this tool.");
                    event.setCancelled(true);
                }
            }
        }
    }
}
