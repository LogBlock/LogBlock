package de.diddiz.util.serializable.itemstack;

import org.bukkit.inventory.ItemStack;

public class SerializableItemStackFactory {

	public static SerializableItemStack makeItemStack(ItemStack itemStack, boolean wasTaken) {

		return new SerializableItemStack_G1(itemStack, wasTaken);
	}
}
