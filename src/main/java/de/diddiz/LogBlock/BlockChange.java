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
import de.diddiz.LogBlock.util.BukkitUtils;
import de.diddiz.LogBlock.util.Utils;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
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
import org.bukkit.inventory.ItemStack;

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
            ItemStack stack = Utils.loadItemStack(rs.getBytes("item"));
            if (stack != null) {
                catemp = new ChestAccess(stack, rs.getBoolean("itemremove"), rs.getInt("itemtype"));
            }
        }
        ca = catemp;
    }

    private String getTypeDetails(BlockData type, byte[] typeState) {
        return getTypeDetails(type, typeState, null, null);
    }

    private String getTypeDetails(BlockData type, byte[] typeState, BlockData oldType, byte[] oldTypeState) {
        String typeDetails = null;

        if (BlockStateCodecs.hasCodec(type.getMaterial())) {
            try {
                typeDetails = BlockStateCodecs.toString(type.getMaterial(), Utils.deserializeYamlConfiguration(typeState), type.equals(oldType) ? Utils.deserializeYamlConfiguration(oldTypeState) : null);
            } catch (Exception e) {
                LogBlock.getInstance().getLogger().log(Level.SEVERE, "Could not parse BlockState for " + type.getMaterial(), e);
            }
        }

        if (typeDetails == null) {
            return "";
        } else {
            return " " + typeDetails;
        }
    }

    @Override
    public String toString() {
        return BaseComponent.toPlainText(getLogMessage(-1));
    }

    @Override
    public BaseComponent[] getLogMessage(int entry) {
        TextComponent msg = new TextComponent();
        if (date > 0) {
            msg.addExtra(prettyDate(date));
            msg.addExtra(" ");
        }
        if (actor != null) {
            msg.addExtra(actor.getName());
            msg.addExtra(" ");
        }
        BlockData type = getBlockSet();
        BlockData replaced = getBlockReplaced();
        if (type == null || replaced == null) {
            msg.addExtra("did an unknown block modification");
            return new BaseComponent[] { msg };
        }

        // Process type details once for later use.
        String typeDetails = getTypeDetails(type, typeState, replaced, replacedState);
        String replacedDetails = getTypeDetails(replaced, replacedState);

        boolean isTypeEqualsReplaced = type.getMaterial().equals(replaced.getMaterial());
        boolean isCakeWithCandle = (type.getMaterial() == Material.CAKE && BukkitUtils.isCandleCake(replaced.getMaterial()));
        if (isTypeEqualsReplaced || isCakeWithCandle) {
            if (BukkitUtils.isEmpty(type.getMaterial())) {
                msg.addExtra(createTextComponentWithColor("did an unspecified action", INTERACT.getColor()));
            } else if (ca != null) {
                if (ca.itemStack == null) {
                    msg.addExtra(createTextComponentWithColor("looked inside ", INTERACT.getColor()));
                    msg.addExtra(prettyMaterial(type));
                } else if (ca.remove) {
                    msg.addExtra(createTextComponentWithColor("took ", DESTROY.getColor()));
                    msg.addExtra(BukkitUtils.toString(ca.itemStack));
                    msg.addExtra(createTextComponentWithColor(" from ", DESTROY.getColor()));
                    msg.addExtra(prettyMaterial(type));
                } else {
                    msg.addExtra(createTextComponentWithColor("put ", CREATE.getColor()));
                    msg.addExtra(BukkitUtils.toString(ca.itemStack));
                    msg.addExtra(createTextComponentWithColor(" into ", CREATE.getColor()));
                    msg.addExtra(prettyMaterial(type));
                }
            } else if (isWaterlogged(type, (Waterlogged) replaced)) {
                if (((Waterlogged) type).isWaterlogged()) {
                    msg.addExtra(createTextComponentWithColor("waterlogged ", CREATE.getColor()));
                    msg.addExtra(prettyMaterial(type));
                } else {
                    msg.addExtra(createTextComponentWithColor("dried ", DESTROY.getColor()));
                    msg.addExtra(prettyMaterial(type));
                }
            } else if (BukkitUtils.getContainerBlocks().contains(type.getMaterial())) {
                msg.addExtra(createTextComponentWithColor("opened ", INTERACT.getColor()));
                msg.addExtra(prettyMaterial(type));
            } else if (type instanceof Openable && ((Openable) type).isOpen() != ((Openable) replaced).isOpen()) {
                // Door, Trapdoor, Fence gate
                msg.addExtra(createTextComponentWithColor(((Openable) type).isOpen() ? "opened " : "closed ", INTERACT.getColor()));
                msg.addExtra(prettyMaterial(type));
            } else if (type.getMaterial() == Material.LEVER && ((Switch) type).isPowered() != ((Switch) replaced).isPowered()) {
                msg.addExtra(createTextComponentWithColor("switched ", INTERACT.getColor()));
                msg.addExtra(prettyMaterial(type));
                msg.addExtra(prettyState(((Switch) type).isPowered() ? " on" : " off"));
            } else if (type instanceof Switch && ((Switch) type).isPowered() != ((Switch) replaced).isPowered()) {
                msg.addExtra(createTextComponentWithColor("pressed ", INTERACT.getColor()));
                msg.addExtra(prettyMaterial(type));
            } else if (type.getMaterial() == Material.CAKE) {
                msg.addExtra(createTextComponentWithColor("ate a piece of ", DESTROY.getColor()));
                msg.addExtra(prettyMaterial(type));
            } else if (type.getMaterial() == Material.NOTE_BLOCK) {
                Note note = ((NoteBlock) type).getNote();
                msg.addExtra(createTextComponentWithColor("set ", INTERACT.getColor()));
                msg.addExtra(prettyMaterial(type));
                msg.addExtra(" to ");
                msg.addExtra(prettyState(note.getTone().name() + (note.isSharped() ? "#" : "")));
            } else if (type.getMaterial() == Material.REPEATER) {
                msg.addExtra(createTextComponentWithColor("set ", INTERACT.getColor()));
                msg.addExtra(prettyMaterial(type));
                msg.addExtra(" to ");
                msg.addExtra(prettyState(((Repeater) type).getDelay()));
                msg.addExtra(createTextComponentWithColor(" ticks delay", DEFAULT.getColor()));
            } else if (type.getMaterial() == Material.COMPARATOR) {
                msg.addExtra(createTextComponentWithColor("set ", INTERACT.getColor()));
                msg.addExtra(prettyMaterial(type));
                msg.addExtra(" to ");
                msg.addExtra(prettyState(((Comparator) type).getMode()));
            } else if (type.getMaterial() == Material.DAYLIGHT_DETECTOR) {
                msg.addExtra(createTextComponentWithColor("set ", INTERACT.getColor()));
                msg.addExtra(prettyMaterial(type));
                msg.addExtra(" to ");
                msg.addExtra(prettyState(((DaylightDetector) type).isInverted() ? "inverted" : "normal"));
            } else if (type instanceof Lectern) {
                msg.addExtra(createTextComponentWithColor("changed the book on a ", INTERACT.getColor()));
                msg.addExtra(prettyMaterial(type));
                msg.addExtra(" to");
                msg.addExtra(prettyState(typeDetails.length() == 0 ? " empty" : typeDetails));
            } else if (type instanceof Powerable) {
                msg.addExtra(createTextComponentWithColor("stepped on ", INTERACT.getColor()));
                msg.addExtra(prettyMaterial(type));
            } else if (type.getMaterial() == Material.TRIPWIRE) {
                msg.addExtra(createTextComponentWithColor("ran into ", INTERACT.getColor()));
                msg.addExtra(prettyMaterial(type));
            } else if (type instanceof Sign || type instanceof WallSign) {
                msg.addExtra(createTextComponentWithColor("edited a ", CREATE.getColor()));
                msg.addExtra(prettyMaterial(type));
                msg.addExtra(createTextComponentWithColor(" to", CREATE.getColor()));
                msg.addExtra(prettyState(typeDetails));
            } else if (type instanceof Candle && ((Candle) type).getCandles() != ((Candle) replaced).getCandles()) {
                msg.addExtra(createTextComponentWithColor("added a candle to ", CREATE.getColor()));
                msg.addExtra(prettyMaterial(type));
            } else if ((type instanceof Candle || BukkitUtils.isCandleCake(type.getMaterial())) && ((Lightable) type).isLit() != ((Lightable) replaced).isLit()) {
                if (((Lightable) type).isLit()) {
                    msg.addExtra(createTextComponentWithColor("lit a ", CREATE.getColor()));
                    msg.addExtra(prettyMaterial(type));
                } else {
                    msg.addExtra(createTextComponentWithColor("extinguished a ", CREATE.getColor()));
                    msg.addExtra(prettyMaterial(type));
                }
            } else {
                msg.addExtra(createTextComponentWithColor("replaced ", CREATE.getColor()));
                msg.addExtra(prettyMaterial(replaced));
                msg.addExtra(prettyState(replacedDetails));
                msg.addExtra(createTextComponentWithColor(" with ", CREATE.getColor()));
                msg.addExtra(prettyMaterial(type));
                msg.addExtra(prettyState(typeDetails));
            }
        }
        else if (BukkitUtils.isEmpty(type.getMaterial())) {
            msg.addExtra(createTextComponentWithColor("destroyed ", DESTROY.getColor()));
            msg.addExtra(prettyMaterial(replaced));
            msg.addExtra(prettyState(replacedDetails));
        }
        else if (BukkitUtils.isEmpty(replaced.getMaterial())) {
            msg.addExtra(createTextComponentWithColor("created ", CREATE.getColor()));
            msg.addExtra(prettyMaterial(type));
            msg.addExtra(prettyState(typeDetails));
        }
        else {
            msg.addExtra(createTextComponentWithColor("replaced ", CREATE.getColor()));
            msg.addExtra(prettyMaterial(replaced));
            msg.addExtra(prettyState(replacedDetails));
            msg.addExtra(createTextComponentWithColor(" with ", CREATE.getColor()));
            msg.addExtra(prettyMaterial(type));
            msg.addExtra(prettyState(typeDetails));
        }
        if (loc != null) {
            msg.addExtra(" at ");
            msg.addExtra(prettyLocation(loc, entry));
        }
        return new BaseComponent[] { msg };
    }

    private static boolean isWaterlogged(BlockData type, Waterlogged replaced) {
        return type instanceof Waterlogged && ((Waterlogged) type).isWaterlogged() != replaced.isWaterlogged();
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
