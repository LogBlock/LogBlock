package de.diddiz.LogBlock.platform;

import de.diddiz.LogBlock.componentwrapper.Component;
import org.bukkit.block.data.type.Shelf;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;

public class TestPlatformAdapter implements PlatformAdapter {
    @Override public void sendComponent(CommandSender target, Component message) { }
    @Override public Component fromLegacy(String text) { return null; }
    @Override public String toPlainText(Component component) { return ""; }
    @Override public ShelfSideChain getShelfSideChain(Shelf shelf) { return ShelfSideChain.UNCONNECTED; }
    @Override public DripstoneThickness getDripstoneThickness(BlockData dripstone) { return DripstoneThickness.TIP; }
    @Override public void setDripstoneThickness(BlockData dripstone, DripstoneThickness thickness) { }
    @Override public BlockFace getDripstoneVerticalDirection(BlockData dripstone) { return BlockFace.UP; }
}
