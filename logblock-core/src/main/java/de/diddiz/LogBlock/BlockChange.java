package de.diddiz.LogBlock;

import static de.diddiz.LogBlock.util.ActionColor.CREATE;
import static de.diddiz.LogBlock.util.ActionColor.DESTROY;
import static de.diddiz.LogBlock.util.ActionColor.INTERACT;
import static de.diddiz.LogBlock.util.MessagingUtil.createTextComponentWithColor;
import static de.diddiz.LogBlock.util.MessagingUtil.prettyDate;
import static de.diddiz.LogBlock.util.MessagingUtil.prettyLocation;
import static de.diddiz.LogBlock.util.MessagingUtil.prettyMaterial;
import static de.diddiz.LogBlock.util.MessagingUtil.prettyState;
import static de.diddiz.LogBlock.util.TypeColor.DEFAULT;

import de.diddiz.LogBlock.blockstate.BlockStateCodecs;
import de.diddiz.LogBlock.componentwrapper.Component;
import de.diddiz.LogBlock.componentwrapper.Components;
import de.diddiz.LogBlock.util.BukkitUtils;
import de.diddiz.LogBlock.util.ItemStackAndAmount;
import de.diddiz.LogBlock.util.Utils;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Lightable;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.Powerable;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Candle;
import org.bukkit.block.data.type.Comparator;
import org.bukkit.block.data.type.DaylightDetector;
import org.bukkit.block.data.type.Lectern;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.block.data.type.Repeater;
import org.bukkit.block.data.type.Sign;
import org.bukkit.block.data.type.Switch;
import org.bukkit.block.data.type.WallSign;

public class BlockChange implements LookupCacheElement {
    public final long id, date;
    public final Location loc;
    public final Actor actor;
    public final String playerName;
    public final int replacedMaterial, replacedData, typeMaterial, typeData;
    public final byte[] replacedState, typeState;
    public final ChestAccess ca;

    public BlockChange(long date, Location loc, Actor actor, int replaced, int replacedData, byte[] replacedState, int type, int typeData, byte[] typeState, ChestAccess ca) {
        id = 0;
        this.date = date;
        this.loc = loc;
        this.actor = actor;
        this.replacedMaterial = replaced;
        this.replacedData = replacedData;
        this.replacedState = replacedState;
        this.typeMaterial = type;
        this.typeData = typeData;
        this.typeState = typeState;
        this.ca = ca;
        this.playerName = actor == null ? null : actor.getName();
    }

    public BlockChange(ResultSet rs, QueryParams p) throws SQLException {
        id = p.needId ? rs.getLong("id") : 0;
        date = p.needDate ? rs.getTimestamp("date").getTime() : 0;
        loc = p.needCoords ? new Location(p.world, rs.getInt("x"), rs.getInt("y"), rs.getInt("z")) : null;
        actor = p.needPlayer ? new Actor(rs) : null;
        playerName = p.needPlayer ? rs.getString("playername") : null;
        replacedMaterial = p.needType ? rs.getInt("replaced") : 0;
        replacedData = p.needType ? rs.getInt("replacedData") : -1;
        typeMaterial = p.needType ? rs.getInt("type") : 0;
        typeData = p.needType ? rs.getInt("typeData") : -1;
        replacedState = p.needType ? rs.getBytes("replacedState") : null;
        typeState = p.needType ? rs.getBytes("typeState") : null;
        ChestAccess catemp = null;
        if (p.needChestAccess) {
            ItemStackAndAmount stack = Utils.loadItemStack(rs.getBytes("item"));
            if (stack != null) {
                catemp = new ChestAccess(stack, rs.getBoolean("itemremove"), rs.getInt("itemtype"));
            }
        }
        ca = catemp;
    }

    private Component getTypeDetails(BlockData type, byte[] typeState) {
        return getTypeDetails(type, typeState, null, null);
    }

    private Component getTypeDetails(BlockData type, byte[] typeState, BlockData oldType, byte[] oldTypeState) {
        Component typeDetails = null;

        if (BlockStateCodecs.hasCodec(type.getMaterial())) {
            try {
                typeDetails = BlockStateCodecs.getChangesAsComponent(type.getMaterial(), Utils.deserializeYamlConfiguration(typeState), type.equals(oldType) ? Utils.deserializeYamlConfiguration(oldTypeState) : null);
            } catch (Exception e) {
                LogBlock.getInstance().getLogger().log(Level.SEVERE, "Could not parse BlockState for " + type.getMaterial(), e);
            }
        }

        if (typeDetails == null) {
            return Components.empty();
        } else {
            Component component = Components.space();
            component = component.append(typeDetails);
            return component;
        }
    }

    @Override
    public String toString() {
        return Components.toPlainText(getLogMessage(-1));
    }

    @Override
    public Component getLogMessage(int entry) {
        Component msg = Components.empty();
        if (date > 0) {
            msg = msg.append(prettyDate(date));
            msg = msg.append(" ");
        }
        if (actor != null) {
            msg = msg.append(actor.getName());
            msg = msg.append(" ");
        }
        BlockData type = getBlockSet();
        BlockData replaced = getBlockReplaced();
        if (type == null || replaced == null) {
            msg = msg.append("did an unknown block modification");
            return msg;
        }

        // Process type details once for later use.
        Component typeDetails = getTypeDetails(type, typeState, replaced, replacedState);
        Component replacedDetails = getTypeDetails(replaced, replacedState);

        if (type.getMaterial().equals(replaced.getMaterial()) || (type.getMaterial() == Material.CAKE && BukkitUtils.isCandleCake(replaced.getMaterial()))) {
            if (BukkitUtils.isEmpty(type.getMaterial())) {
                msg = msg.append(createTextComponentWithColor("did an unspecified action", INTERACT.getColor()));
            } else if (ca != null) {
                if (ca.itemStack == null) {
                    msg = msg.append(createTextComponentWithColor("looked inside ", INTERACT.getColor()));
                    msg = msg.append(prettyMaterial(type));
                } else if (ca.remove) {
                    msg = msg.append(createTextComponentWithColor("took ", DESTROY.getColor()));
                    msg = msg.append(BukkitUtils.toString(ca.itemStack));
                    msg = msg.append(createTextComponentWithColor(" from ", DESTROY.getColor()));
                    msg = msg.append(prettyMaterial(type));
                } else {
                    msg = msg.append(createTextComponentWithColor("put ", CREATE.getColor()));
                    msg = msg.append(BukkitUtils.toString(ca.itemStack));
                    msg = msg.append(createTextComponentWithColor(" into ", CREATE.getColor()));
                    msg = msg.append(prettyMaterial(type));
                }
            } else if (type instanceof Waterlogged && ((Waterlogged) type).isWaterlogged() != ((Waterlogged) replaced).isWaterlogged()) {
                if (((Waterlogged) type).isWaterlogged()) {
                    msg = msg.append(createTextComponentWithColor("waterlogged ", CREATE.getColor()));
                    msg = msg.append(prettyMaterial(type));
                } else {
                    msg = msg.append(createTextComponentWithColor("dried ", DESTROY.getColor()));
                    msg = msg.append(prettyMaterial(type));
                }
            } else if (BukkitUtils.isContainerBlock(type.getMaterial())) {
                msg = msg.append(createTextComponentWithColor("opened ", INTERACT.getColor()));
                msg = msg.append(prettyMaterial(type));
            } else if (type instanceof Openable && ((Openable) type).isOpen() != ((Openable) replaced).isOpen()) {
                // Door, Trapdoor, Fence gate
                msg = msg.append(createTextComponentWithColor(((Openable) type).isOpen() ? "opened " : "closed ", INTERACT.getColor()));
                msg = msg.append(prettyMaterial(type));
            } else if (type.getMaterial() == Material.LEVER && ((Switch) type).isPowered() != ((Switch) replaced).isPowered()) {
                msg = msg.append(createTextComponentWithColor("switched ", INTERACT.getColor()));
                msg = msg.append(prettyMaterial(type));
                msg = msg.append(prettyState(((Switch) type).isPowered() ? " on" : " off"));
            } else if (type instanceof Switch && ((Switch) type).isPowered() != ((Switch) replaced).isPowered()) {
                msg = msg.append(createTextComponentWithColor("pressed ", INTERACT.getColor()));
                msg = msg.append(prettyMaterial(type));
            } else if (type.getMaterial() == Material.CAKE) {
                msg = msg.append(createTextComponentWithColor("ate a piece of ", DESTROY.getColor()));
                msg = msg.append(prettyMaterial(type));
            } else if (type.getMaterial() == Material.NOTE_BLOCK) {
                Note note = ((NoteBlock) type).getNote();
                msg = msg.append(createTextComponentWithColor("set ", INTERACT.getColor()));
                msg = msg.append(prettyMaterial(type));
                msg = msg.append(" to ");
                msg = msg.append(prettyState(note.getTone().name() + (note.isSharped() ? "#" : "")));
            } else if (type.getMaterial() == Material.REPEATER) {
                msg = msg.append(createTextComponentWithColor("set ", INTERACT.getColor()));
                msg = msg.append(prettyMaterial(type));
                msg = msg.append(" to ");
                msg = msg.append(prettyState(((Repeater) type).getDelay()));
                msg = msg.append(createTextComponentWithColor(" ticks delay", DEFAULT.getColor()));
            } else if (type.getMaterial() == Material.COMPARATOR) {
                msg = msg.append(createTextComponentWithColor("set ", INTERACT.getColor()));
                msg = msg.append(prettyMaterial(type));
                msg = msg.append(" to ");
                msg = msg.append(prettyState(((Comparator) type).getMode()));
            } else if (type.getMaterial() == Material.DAYLIGHT_DETECTOR) {
                msg = msg.append(createTextComponentWithColor("set ", INTERACT.getColor()));
                msg = msg.append(prettyMaterial(type));
                msg = msg.append(" to ");
                msg = msg.append(prettyState(((DaylightDetector) type).isInverted() ? "inverted" : "normal"));
            } else if (type instanceof Lectern) {
                msg = msg.append(createTextComponentWithColor("changed the book on a ", INTERACT.getColor()));
                msg = msg.append(prettyMaterial(type));
                msg = msg.append(" to");
                msg = msg.append(prettyState(typeDetails));
            } else if (type instanceof Powerable) {
                msg = msg.append(createTextComponentWithColor("stepped on ", INTERACT.getColor()));
                msg = msg.append(prettyMaterial(type));
            } else if (type.getMaterial() == Material.TRIPWIRE) {
                msg = msg.append(createTextComponentWithColor("ran into ", INTERACT.getColor()));
                msg = msg.append(prettyMaterial(type));
            } else if (type instanceof Sign || type instanceof WallSign) {
                msg = msg.append(createTextComponentWithColor("edited a ", CREATE.getColor()));
                msg = msg.append(prettyMaterial(type));
                msg = msg.append(createTextComponentWithColor(" to", CREATE.getColor()));
                msg = msg.append(prettyState(typeDetails));
            } else if (type instanceof Candle && ((Candle) type).getCandles() != ((Candle) replaced).getCandles()) {
                msg = msg.append(createTextComponentWithColor("added a candle to ", CREATE.getColor()));
                msg = msg.append(prettyMaterial(type));
            } else if ((type instanceof Candle || BukkitUtils.isCandleCake(type.getMaterial())) && ((Lightable) type).isLit() != ((Lightable) replaced).isLit()) {
                if (((Lightable) type).isLit()) {
                    msg = msg.append(createTextComponentWithColor("lit a ", CREATE.getColor()));
                    msg = msg.append(prettyMaterial(type));
                } else {
                    msg = msg.append(createTextComponentWithColor("extinguished a ", CREATE.getColor()));
                    msg = msg.append(prettyMaterial(type));
                }
            } else {
                msg = msg.append(createTextComponentWithColor("replaced ", CREATE.getColor()));
                msg = msg.append(prettyMaterial(replaced));
                msg = msg.append(prettyState(replacedDetails));
                msg = msg.append(createTextComponentWithColor(" with ", CREATE.getColor()));
                msg = msg.append(prettyMaterial(type));
                msg = msg.append(prettyState(typeDetails));
            }
        } else if (BukkitUtils.isEmpty(type.getMaterial())) {
            msg = msg.append(createTextComponentWithColor("destroyed ", DESTROY.getColor()));
            msg = msg.append(prettyMaterial(replaced));
            msg = msg.append(prettyState(replacedDetails));
        } else if (BukkitUtils.isEmpty(replaced.getMaterial())) {
            msg = msg.append(createTextComponentWithColor("created ", CREATE.getColor()));
            msg = msg.append(prettyMaterial(type));
            msg = msg.append(prettyState(typeDetails));
        } else {
            msg = msg.append(createTextComponentWithColor("replaced ", CREATE.getColor()));
            msg = msg.append(prettyMaterial(replaced));
            msg = msg.append(prettyState(replacedDetails));
            msg = msg.append(createTextComponentWithColor(" with ", CREATE.getColor()));
            msg = msg.append(prettyMaterial(type));
            msg = msg.append(prettyState(typeDetails));
        }
        if (loc != null) {
            msg = msg.append(" at ");
            msg = msg.append(prettyLocation(loc, entry));
        }
        return msg;
    }

    public BlockData getBlockReplaced() {
        return MaterialConverter.getBlockData(replacedMaterial, replacedData);
    }

    public BlockData getBlockSet() {
        return MaterialConverter.getBlockData(typeMaterial, typeData);
    }

    @Override
    public Location getLocation() {
        return loc;
    }
}
