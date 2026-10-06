package de.diddiz.LogBlock.util;

import org.bukkit.inventory.ItemStack;

public record ItemStackAndAmount(ItemStack stack, int amount) {

    public static ItemStackAndAmount fromStack(ItemStack stack) {
        int amount = stack.getAmount();
        if (amount > 1) {
            stack = stack.clone();
            stack.setAmount(1);
        }
        return new ItemStackAndAmount(stack, amount);
    }
}
