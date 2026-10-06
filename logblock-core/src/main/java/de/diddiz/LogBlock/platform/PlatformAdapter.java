package de.diddiz.LogBlock.platform;

import de.diddiz.LogBlock.componentwrapper.Component;
import org.bukkit.block.data.type.Shelf;
import org.bukkit.block.data.type.PointedDripstone;
import org.bukkit.command.CommandSender;

/** Operations whose implementations depend on the server API. */
public interface PlatformAdapter {
    void sendComponent(CommandSender target, Component message);
    Component fromLegacy(String text);
    String toPlainText(Component component);
    ShelfSideChain getShelfSideChain(Shelf shelf);
    DripstoneThickness getDripstoneThickness(PointedDripstone dripstone);
    void setDripstoneThickness(PointedDripstone dripstone, DripstoneThickness thickness);
}
