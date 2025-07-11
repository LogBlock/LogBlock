package de.diddiz.LogBlock.util;

import static de.diddiz.LogBlock.util.ActionColor.CREATE;
import static de.diddiz.LogBlock.util.ActionColor.DESTROY;
import static de.diddiz.LogBlock.util.TypeColor.DEFAULT;
import static de.diddiz.LogBlock.util.Utils.spaces;

import de.diddiz.LogBlock.componentwrapper.ChatColor;
import de.diddiz.LogBlock.componentwrapper.Click;
import de.diddiz.LogBlock.componentwrapper.Component;
import de.diddiz.LogBlock.componentwrapper.Components;
import de.diddiz.LogBlock.componentwrapper.Hover;
import de.diddiz.LogBlock.config.Config;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;

public class MessagingUtil {
    public static Component formatSummarizedChanges(int created, int destroyed, Component actor, int createdWidth, int destroyedWidth, float spaceFactor) {
        Component textCreated = createTextComponentWithColor(created + spaces((int) ((10 - String.valueOf(created).length()) / spaceFactor)), CREATE.getColor());
        Component textDestroyed = createTextComponentWithColor(destroyed + spaces((int) ((10 - String.valueOf(destroyed).length()) / spaceFactor)), DESTROY.getColor());
        Component result = Components.empty();
        result = result.append(textCreated);
        result = result.append(textDestroyed);
        result = result.append(actor);
        return result;
    }

    public static Component createTextComponentWithColor(String text, ChatColor color) {
        return Components.text(text, color);
    }

    public static Component brackets(BracketType type, Component... content) {
        Component tc = createTextComponentWithColor(type.getStarting(), TypeColor.BRACKETS.getColor());
        for (Component c : content) {
            tc = tc.append(c);
        }
        tc = tc.append(Components.text(type.getEnding()));
        return tc;
    }

    public static Component prettyDate(long date) {
        Component tc = brackets(BracketType.STANDARD, createTextComponentWithColor(Config.formatterShort.format(date), TypeColor.DATE.getColor()));
        tc = tc.hover(Hover.text(Config.formatter.format(date)));
        return tc;
    }

    public static Component prettyState(String stateName) {
        return createTextComponentWithColor(stateName, TypeColor.STATE.getColor());
    }

    public static Component prettyState(Component stateName) {
        Component tc = Components.text("", TypeColor.STATE.getColor());
        if (stateName != null) {
            tc = tc.append(stateName);
        }
        return tc;
    }

    public static Component prettyState(int stateValue) {
        return prettyState(Integer.toString(stateValue));
    }

    public static <E extends Enum<E>> Component prettyState(E enumerator) {
        return prettyState(enumerator.toString());
    }

    public static Component prettyMaterial(String materialName) {
        return createTextComponentWithColor(materialName.toUpperCase(), TypeColor.MATERIAL.getColor());
    }

    public static Component prettyMaterial(Material material) {
        return prettyMaterial(material.name());
    }

    public static Component prettyMaterial(BlockData material) {
        Component tc = prettyMaterial(material.getMaterial());
        String bdString = material.getAsString();
        int bracket = bdString.indexOf("[");
        if (bracket >= 0) {
            int bracket2 = bdString.indexOf("]", bracket);
            if (bracket2 >= 0) {
                String state = bdString.substring(bracket + 1, bracket2).replace(',', '\n');
                tc = tc.hover(Hover.text(state));
            }
        }
        return tc;
    }

    public static Component prettyEntityType(EntityType type) {
        return prettyMaterial(type.name());
    }

    public static Component prettyLocation(Location loc, int entryId) {
        return prettyLocation(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), entryId);
    }

    public static Component prettyLocation(int x, int y, int z, int entryId) {
        Component tc = createTextComponentWithColor("", DEFAULT.getColor());
        tc = tc.append(createTextComponentWithColor(Integer.toString(x), TypeColor.COORDINATE.getColor()));
        tc = tc.append(createTextComponentWithColor(", ", DEFAULT.getColor()));
        tc = tc.append(createTextComponentWithColor(Integer.toString(y), TypeColor.COORDINATE.getColor()));
        tc = tc.append(createTextComponentWithColor(", ", DEFAULT.getColor()));
        tc = tc.append(createTextComponentWithColor(Integer.toString(z), TypeColor.COORDINATE.getColor()));
        if (entryId > 0) {
            tc = tc.click(Click.run("/lb tp " + entryId));
            tc = tc.hover(Hover.text("Teleport here"));
        }
        return tc;
    }

    public enum BracketType {
        STANDARD("[", "]"),
        ANGLE("<", ">");

        private String starting, ending;

        BracketType(String starting, String ending) {
            this.starting = starting;
            this.ending = ending;
        }

        public String getStarting() {
            return starting;
        }

        public String getEnding() {
            return ending;
        }
    }
}
