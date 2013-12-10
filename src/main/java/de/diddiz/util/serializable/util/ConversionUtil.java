package de.diddiz.util.serializable.util;

import de.diddiz.LogBlock.QueryParams;
import de.diddiz.util.serializable.itemstack.SerializableItemStack;
import de.diddiz.util.serializable.itemstack.SerializableItemStackFactory;
import de.diddiz.util.serializable.itemstack.SerializableItemStack_G1;
import org.bukkit.inventory.ItemStack;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ConversionUtil {

	public static SerializableItemStack grabFromRS(ResultSet rs, QueryParams params) throws SQLException {

		SerializableItemStack is = null;

		if (params.needSpecificItemData) {
			InputStream dbis = rs.getBinaryStream("item");
			if (dbis != null) {
				ObjectInputStream ois = null;
				try {
					ois = new ObjectInputStream(dbis);
					Object object = ois.readObject();

					if (object instanceof SerializableItemStack_G1) {
						is = (SerializableItemStack) object;
					}
				} catch (FileNotFoundException ignored) {
				} catch (ClassNotFoundException ignored) {
				} catch (IOException ignored) {
				} finally {
					if (ois != null) {
						try {
							ois.close();
						} catch (IOException ignored) {
						}
					}
				}
			}
		}

		if (is == null && params.needGeneralItemData) {
			is = SerializableItemStackFactory.makeItemStack(rs.getInt("itemtype"), rs.getInt("itemamount"), rs.getShort("itemdata"));
		}
		return is;
	}
}
