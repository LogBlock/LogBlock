package de.diddiz.LogBlock.events;
import de.diddiz.LogBlock.ChestAccess;
import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.event.HandlerList;

public class BlockChangePreLogEvent extends PreLogEvent {

	private static final HandlerList handlers = new HandlerList();
	private Location location;
	private int typeBefore, typeAfter;
	private byte data;
	private String signText;
	private ChestAccess chestAccess;

	public BlockChangePreLogEvent(String owner, Location location, int typeBefore, int typeAfter, byte data,
								  String signText, ChestAccess chestAccess) {

		super(owner);
		this.location = location;
		this.typeBefore = typeBefore;
		this.typeAfter = typeAfter;
		this.data = data;
		this.signText = signText;
		this.chestAccess = chestAccess;
	}

	public Location getLocation() {

		return location;
	}

	public void setLocation(Location location) {

		this.location = location;
	}

	public int getTypeBefore() {

		return typeBefore;
	}

	public void setTypeBefore(int typeBefore) {

		this.typeBefore = typeBefore;
	}

	public int getTypeAfter() {

		return typeAfter;
	}

	public void setTypeAfter(int typeAfter) {

		this.typeAfter = typeAfter;
	}

	public byte getData() {

		return data;
	}

	public void setData(byte data) {

		this.data = data;
	}

	public String getSignText() {

		return signText;
	}

	public void setSignText(String[] signText) {

		if (signText != null) {
			// Check for block
			Validate.isTrue(isValidSign(), "Must be valid sign block");

			// Check for problems
			Validate.noNullElements(signText, "No null lines");
			Validate.isTrue(signText.length == 4, "Sign text must be 4 strings");

			this.signText = signText[0] + "\0" + signText[1] + "\0" + signText[2] + "\0" + signText[3];
		} else {
			this.signText = null;
		}
	}

	private boolean isValidSign() {

		if ((typeAfter == 63 || typeAfter == 68) && typeBefore == 0) return true;
		if ((typeBefore == 63 || typeBefore == 68) && typeAfter == 0) return true;
		if ((typeAfter == 63 || typeAfter == 68) && typeBefore == typeAfter) return true;
		return false;
	}

	public ChestAccess getChestAccess() {

		return chestAccess;
	}

	public void setChestAccess(ChestAccess chestAccess) {

		this.chestAccess = chestAccess;
	}

	public HandlerList getHandlers() {

		return handlers;
	}

	public static HandlerList getHandlerList() {

		return handlers;
	}
}
