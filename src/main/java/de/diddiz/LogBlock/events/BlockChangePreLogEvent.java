package de.diddiz.LogBlock.events;

import de.diddiz.LogBlock.Actor;
import de.diddiz.LogBlock.ChestAccess;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.HandlerList;

public class BlockChangePreLogEvent extends PreLogEvent {

    private static final HandlerList handlers = new HandlerList();
    private Location location;
    private BlockData typeBefore, typeAfter;
    private String signText;
    private ChestAccess chestAccess;

    public BlockChangePreLogEvent(Actor owner, Location location, BlockData typeBefore, BlockData typeAfter,
                                  String signText, ChestAccess chestAccess) {

        super(owner);
        this.location = location;
        this.typeBefore = typeBefore;
        this.typeAfter = typeAfter;
        this.signText = signText;
        this.chestAccess = chestAccess;
    }

    public Location getLocation() {

        return location;
    }

    public void setLocation(Location location) {

        this.location = location;
    }

    public BlockData getTypeBefore() {

        return typeBefore;
    }

    public void setTypeBefore(BlockData typeBefore) {
        if (typeBefore == null) {
            typeBefore = Bukkit.createBlockData(Material.AIR);
        }
        this.typeBefore = typeBefore;
    }

    public BlockData getTypeAfter() {

        return typeAfter;
    }

    public void setTypeAfter(BlockData typeAfter) {
        if (typeAfter == null) {
            typeAfter = Bukkit.createBlockData(Material.AIR);
        }
        this.typeAfter = typeAfter;
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

        if ((typeAfter.getMaterial() == Material.SIGN || typeAfter.getMaterial() == Material.WALL_SIGN) && typeBefore.getMaterial() == Material.AIR) {
            return true;
        }
        if ((typeBefore.getMaterial() == Material.SIGN || typeBefore.getMaterial() == Material.WALL_SIGN) && typeAfter.getMaterial() == Material.AIR) {
            return true;
        }
        if ((typeAfter.getMaterial() == Material.SIGN || typeAfter.getMaterial() == Material.WALL_SIGN) && typeBefore.equals(typeAfter)) {
            return true;
        }
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
