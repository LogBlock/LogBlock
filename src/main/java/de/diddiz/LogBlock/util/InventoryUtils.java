package de.diddiz.LogBlock.util;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class InventoryUtils {

    public static int addToInventory(Inventory inventory, ItemStackAndAmount item) {
        if (item == null || item.stack() == null || item.stack().getType() == Material.AIR) {
            return 0;
        }
        int maxStackSize = Math.max(Math.min(inventory.getMaxStackSize(), item.stack().getMaxStackSize()), 1);

        ItemStack[] contents = inventory.getStorageContents();

        int remaining = item.amount();
        int initialRemaining = remaining;

        // fill partial stacks
        int firstPartial = -1;
        while (remaining > 0) {
            firstPartial = getFirstPartial(item.stack(), maxStackSize, contents, firstPartial + 1);
            if (firstPartial < 0) {
                break;
            }
            ItemStack content = contents[firstPartial];
            int add = Math.min(maxStackSize - content.getAmount(), remaining);
            content.setAmount(content.getAmount() + add);
            remaining -= add;
        }
        // create new stacks
        int firstFree = -1;
        while (remaining > 0) {
            firstFree = getFirstFree(contents, firstFree + 1);
            if (firstFree < 0) {
                break;
            }
            ItemStack content = item.stack().clone();
            contents[firstFree] = content;
            int add = Math.min(maxStackSize, remaining);
            content.setAmount(add);
            remaining -= add;
        }

        if (remaining < initialRemaining) {
            inventory.setStorageContents(contents);
        }
        return remaining;
    }

    public static int removeFromInventory(Inventory inventory, ItemStackAndAmount item) {
        if (item == null || item.stack() == null || item.stack().getType() == Material.AIR) {
            return 0;
        }

        ItemStack[] contents = inventory.getStorageContents();
        int remaining = item.amount();
        int initialRemaining = remaining;

        int firstSimilar = -1;
        while (remaining > 0) {
            firstSimilar = getFirstSimilar(item.stack(), contents, firstSimilar + 1);
            if (firstSimilar < 0) {
                break;
            }
            ItemStack content = contents[firstSimilar];
            int here = content.getAmount();
            if (here > remaining) {
                content.setAmount(here - remaining);
                remaining = 0;
            } else {
                contents[firstSimilar] = null;
                remaining -= here;
            }
        }

        if (remaining < initialRemaining) {
            inventory.setStorageContents(contents);
        }
        return remaining;
    }

    private static int getFirstSimilar(ItemStack item, ItemStack[] contents, int start) {
        for (int i = start; i < contents.length; i++) {
            ItemStack content = contents[i];
            if (content != null && content.isSimilar(item)) {
                return i;
            }
        }
        return -1;
    }

    private static int getFirstPartial(ItemStack item, int maxStackSize, ItemStack[] contents, int start) {
        for (int i = start; i < contents.length; i++) {
            ItemStack content = contents[i];
            if (content != null && content.isSimilar(item) && content.getAmount() < maxStackSize) {
                return i;
            }
        }
        return -1;
    }

    private static int getFirstFree(ItemStack[] contents, int start) {
        for (int i = start; i < contents.length; i++) {
            ItemStack content = contents[i];
            if (content == null || content.getAmount() == 0 || content.getType() == Material.AIR) {
                return i;
            }
        }
        return -1;
    }
}
