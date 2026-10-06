package de.diddiz.LogBlock;

import de.diddiz.LogBlock.util.ItemStackAndAmount;

public class ChestAccess {
    public final ItemStackAndAmount itemStack;
    public final boolean remove;
    public final int itemType;

    public ChestAccess(ItemStackAndAmount itemStack, boolean remove, int itemType) {
        this.itemStack = itemStack;
        this.remove = remove;
        this.itemType = itemType;
    }
}
