package de.diddiz.LogBlock.platform;

import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.componentwrapper.Component;
import de.diddiz.LogBlock.listeners.AdvancedEntityLogging;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Shelf;
import org.bukkit.command.CommandSender;

/** Operations whose implementations depend on the server API. */
public interface PlatformAdapter {
    void sendComponent(CommandSender target, Component message);
    Component fromLegacy(String text);
    String toPlainText(Component component);
    ShelfSideChain getShelfSideChain(Shelf shelf);
    DripstoneThickness getDripstoneThickness(BlockData dripstone);
    void setDripstoneThickness(BlockData dripstone, DripstoneThickness thickness);
    BlockFace getDripstoneVerticalDirection(BlockData dripstone);

    default void registerEntityLoggingListeners(LogBlock plugin, AdvancedEntityLogging logging) {
    }
}
