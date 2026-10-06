package de.diddiz.LogBlock.listeners;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.Logging;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.minecart.CommandMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

import static de.diddiz.LogBlock.config.Config.isLogging;

public class ChatLogging extends LoggingListener {
    public ChatLogging(LogBlock lb) {
        super(lb);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (isLogging(event.getPlayer().getWorld(), Logging.PLAYER_COMMANDS)) {
            consumer.queueChat(Actor.actorFromEntity(event.getPlayer()), event.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (isLogging(event.getPlayer().getWorld(), Logging.CHAT)) {
            consumer.queueChat(Actor.actorFromEntity(event.getPlayer()), event.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onServerCommand(ServerCommandEvent event) {
        CommandSender sender = event.getSender();
        Actor actor;
        if (sender instanceof BlockCommandSender) {
            if (!isLogging(((BlockCommandSender) sender).getBlock().getWorld(), Logging.COMMANDBLOCK_COMMANDS)) {
                return;
            }
            actor = new Actor("CommandBlock");
        } else if (sender instanceof CommandMinecart) {
            if (!isLogging(((CommandMinecart) sender).getWorld(), Logging.COMMANDBLOCK_COMMANDS)) {
                return;
            }
            actor = new Actor("CommandMinecart");
        } else {
            if (!isLogging(Logging.CONSOLE_COMMANDS)) {
                return;
            }
            actor = new Actor("Console");
        }
        consumer.queueChat(actor, "/" + event.getCommand());
    }
}
