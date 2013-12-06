package de.diddiz.util.serializable.util;

import de.diddiz.util.serializable.itemstack.SerializableItemStack;
import de.diddiz.util.serializable.itemstack.SerializableItemStack_G1;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ConversionUtil {

	public static SerializableItemStack grabFromRS(ResultSet rs) throws SQLException {

		SerializableItemStack is = null;
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
		return is;
	}
}
