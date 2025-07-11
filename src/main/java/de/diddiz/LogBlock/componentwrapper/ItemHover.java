package de.diddiz.LogBlock.componentwrapper;

import org.bukkit.inventory.ItemStack;

public class ItemHover implements Hover {
    private final ItemStack item;

    ItemHover(ItemStack item) {
        this.item = item;
    }

    public ItemStack getItem() {
        return item;
    }
}
