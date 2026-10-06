package de.diddiz.LogBlock.listeners;

import de.diddiz.LogBlock.Consumer;
import de.diddiz.LogBlock.LogBlock;
import org.bukkit.event.Listener;

public class LoggingListener implements Listener {
    protected final Consumer consumer;

    public LoggingListener(LogBlock lb) {
        consumer = lb.getConsumer();
    }
}
