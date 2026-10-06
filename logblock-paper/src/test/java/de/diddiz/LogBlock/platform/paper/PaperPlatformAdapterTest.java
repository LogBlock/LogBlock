package de.diddiz.LogBlock.platform.paper;

import de.diddiz.LogBlock.componentwrapper.ChatColor;
import de.diddiz.LogBlock.componentwrapper.Components;
import de.diddiz.LogBlock.componentwrapper.Hover;
import de.diddiz.LogBlock.componentwrapper.RunCommandClick;
import de.diddiz.LogBlock.platform.ShelfSideChain;
import de.diddiz.LogBlock.platform.DripstoneThickness;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.key.Key;
import java.util.function.UnaryOperator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.block.data.SideChaining;
import org.bukkit.block.data.type.Shelf;
import org.bukkit.block.data.type.PointedDripstone;
import org.bukkit.block.data.type.Speleothem;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.BlockFace;
import org.bukkit.Material;
import org.junit.Test;
import static org.junit.Assert.*;

public class PaperPlatformAdapterTest {
    private final PaperPlatformAdapter adapter = new PaperPlatformAdapter(null);

    @Test public void legacyAndPlainTextRetainColorsAndChildren() {
        var parsed = adapter.fromLegacy("\u00a7cRed \u00a7aGreen");
        assertEquals("Red Green", adapter.toPlainText(parsed));
        var component = Components.text("root", ChatColor.RED).append(Components.text("child", ChatColor.GREEN));
        assertEquals("rootchild", adapter.toPlainText(component));
        var adventure = PaperPlatformAdapter.toAdventure(component);
        assertEquals(NamedTextColor.RED, adventure.color());
        assertEquals(NamedTextColor.GREEN, adventure.children().getFirst().color());
    }

    @Test public void retainsClickAndTextHover() {
        var message = Components.text("lookup", ChatColor.RED, Hover.text("details"), new RunCommandClick("/lb lookup"));
        var adventure = PaperPlatformAdapter.toAdventure(message);
        assertEquals(ClickEvent.runCommand("/lb lookup"), adventure.clickEvent());
        assertEquals(HoverEvent.Action.SHOW_TEXT, adventure.hoverEvent().action());
    }

    @Test public void mapsEveryShelfSideChain() {
        for (SideChaining.ChainPart chain : SideChaining.ChainPart.values()) {
            Shelf shelf = (Shelf) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { Shelf.class },
                    (proxy, method, arguments) -> method.getName().equals("getSideChain") ? chain : null);
            assertEquals(ShelfSideChain.valueOf(chain.name()), adapter.getShelfSideChain(shelf));
        }
    }

    @Test public void readsAndWritesEveryDripstoneThickness() {
        for (Material material : new Material[] { Material.POINTED_DRIPSTONE, Material.SULFUR_SPIKE }) {
            Class<?> api = material == Material.SULFUR_SPIKE ? Speleothem.class : PointedDripstone.class;
            for (BlockFace direction : new BlockFace[] { BlockFace.UP, BlockFace.DOWN }) {
                for (Speleothem.Thickness thickness : Speleothem.Thickness.values()) {
                    AtomicReference<Object> written = new AtomicReference<>();
                    BlockData dripstone = (BlockData) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { api },
                            (proxy, method, arguments) -> {
                                if (method.getName().equals("getMaterial")) return material;
                                if (method.getName().equals("getVerticalDirection")) return direction;
                                if (method.getName().equals("getThickness")) return thickness;
                                if (method.getName().equals("setThickness")) written.set(arguments[0]);
                                return null;
                            });
                    if (material == Material.SULFUR_SPIKE) assertFalse(dripstone instanceof PointedDripstone);
                    DripstoneThickness common = DripstoneThickness.valueOf(thickness.name());
                    assertEquals(common, adapter.getDripstoneThickness(dripstone));
                    assertEquals(direction, adapter.getDripstoneVerticalDirection(dripstone));
                    assertEquals(material, dripstone.getMaterial());
                    adapter.setDripstoneThickness(dripstone, common);
                    assertEquals(thickness, written.get());
                }
            }
        }
    }

    @Test public void retainsItemHover() {
        ItemStack stack = new ItemStack() {
            @Override public HoverEvent<HoverEvent.ShowItem> asHoverEvent(UnaryOperator<HoverEvent.ShowItem> operation) {
                return HoverEvent.showItem(operation.apply(HoverEvent.ShowItem.showItem(Key.key("minecraft:stone"), 1)));
            }
        };
        var message = Components.text("item").hover(Hover.item(stack));
        var event = PaperPlatformAdapter.toAdventure(message).hoverEvent();
        assertEquals(HoverEvent.Action.SHOW_ITEM, event.action());
        assertEquals(Key.key("minecraft:stone"), ((HoverEvent.ShowItem) event.value()).item());
        assertEquals(1, ((HoverEvent.ShowItem) event.value()).count());
    }
}
