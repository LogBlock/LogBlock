package de.diddiz.util.serializable.itemstack;

import org.bukkit.inventory.ItemStack;

import java.io.Serializable;

public interface SerializableItemStack extends Serializable {

	public int getType();
	public int getData();
	public int getAmount();

	public boolean wasAdded();
	public ItemStack toBukkit();
}
