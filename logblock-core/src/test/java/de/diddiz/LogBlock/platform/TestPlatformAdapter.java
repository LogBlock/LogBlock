package de.diddiz.LogBlock.platform;

import de.diddiz.LogBlock.componentwrapper.Component;
import org.bukkit.block.data.type.Shelf;
import org.bukkit.block.data.type.PointedDripstone;
import org.bukkit.command.CommandSender;

public class TestPlatformAdapter implements PlatformAdapter {
    @Override public void sendComponent(CommandSender target, Component message) { }
    @Override public Component fromLegacy(String text) { return null; }
    @Override public String toPlainText(Component component) { return ""; }
    @Override public ShelfSideChain getShelfSideChain(Shelf shelf) { return ShelfSideChain.UNCONNECTED; }
    @Override public DripstoneThickness getDripstoneThickness(PointedDripstone dripstone) { return DripstoneThickness.TIP; }
    @Override public void setDripstoneThickness(PointedDripstone dripstone, DripstoneThickness thickness) { }
}
