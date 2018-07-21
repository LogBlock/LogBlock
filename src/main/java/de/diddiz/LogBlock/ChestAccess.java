package de.diddiz.LogBlock;

import org.bukkit.inventory.ItemStack;

public class ChestAccess {
    final ItemStack itemStack;
    final boolean remove;

    public ChestAccess(ItemStack itemStack, boolean remove) {
        this.itemStack = itemStack;
        this.remove = remove;
    }
}
