package de.diddiz.util.serializable.itemstack;

import org.bukkit.inventory.ItemStack;

import java.io.Serializable;

public interface SerializableItemStack extends Serializable {

	public boolean wasAdded();
	public ItemStack toBukkit();
}
