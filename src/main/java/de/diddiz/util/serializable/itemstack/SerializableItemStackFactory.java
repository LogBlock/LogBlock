package de.diddiz.util.serializable.itemstack;

import org.bukkit.inventory.ItemStack;

public class SerializableItemStackFactory {

	public static SerializableItemStack makeItemStack(ItemStack itemStack, boolean wasAdded) {

		return new SerializableItemStack_G1(itemStack, wasAdded);
	}

	@Deprecated
	public static SerializableItemStack makeItemStack(int type, int amount, short data) {

		boolean wasAdded = amount > 0;

		return makeItemStack(new ItemStack(type, Math.abs(amount), data), wasAdded);
	}
}
