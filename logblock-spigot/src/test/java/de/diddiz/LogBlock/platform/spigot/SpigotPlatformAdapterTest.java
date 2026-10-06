package de.diddiz.LogBlock.platform.spigot;

import de.diddiz.LogBlock.componentwrapper.ChatColor;
import de.diddiz.LogBlock.componentwrapper.Components;
import de.diddiz.LogBlock.componentwrapper.Hover;
import de.diddiz.LogBlock.componentwrapper.RunCommandClick;
import de.diddiz.LogBlock.platform.ShelfSideChain;
import de.diddiz.LogBlock.platform.DripstoneThickness;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.hover.content.Item;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.block.data.type.Shelf;
import org.bukkit.block.data.type.PointedDripstone;
import org.bukkit.command.CommandSender;
import org.junit.Test;
import static org.junit.Assert.*;

public class SpigotPlatformAdapterTest {
    private final SpigotPlatformAdapter adapter = new SpigotPlatformAdapter(null);

    @Test public void legacyAndPlainTextRetainColorsAndChildren() {
        var parsed = adapter.fromLegacy("\u00a7cRed \u00a7aGreen");
        assertEquals("Red Green", adapter.toPlainText(parsed));
        var component = Components.text("root", ChatColor.RED).append(Components.text("child", ChatColor.GREEN));
        assertEquals("rootchild", adapter.toPlainText(component));
        assertEquals(ChatColor.GREEN, component.getChildren().getFirst().getColor());
    }

    @Test public void sendRetainsClickAndTextHover() {
        AtomicReference<BaseComponent> sent = new AtomicReference<>();
        CommandSender.Spigot sender = new CommandSender.Spigot() {
            @Override public void sendMessage(BaseComponent component) { sent.set(component); }
            @Override public void sendMessage(BaseComponent... components) { sent.set(components[0]); }
        };
        CommandSender target = (CommandSender) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { CommandSender.class },
                (proxy, method, arguments) -> method.getName().equals("spigot") ? sender : null);
        var message = Components.text("lookup", ChatColor.RED, Hover.text("details"), new RunCommandClick("/lb lookup"));
        adapter.sendComponent(target, message);
        assertEquals("/lb lookup", sent.get().getClickEvent().getValue());
        assertNotNull(sent.get().getHoverEvent());
        assertEquals(net.md_5.bungee.api.ChatColor.RED, sent.get().getColor());
    }

    @Test public void mapsEveryShelfSideChain() {
        for (Shelf.SideChain chain : Shelf.SideChain.values()) {
            Shelf shelf = (Shelf) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { Shelf.class },
                    (proxy, method, arguments) -> method.getName().equals("getSideChain") ? chain : null);
            assertEquals(ShelfSideChain.valueOf(chain.name()), adapter.getShelfSideChain(shelf));
        }
    }

    @Test public void readsAndWritesEveryDripstoneThickness() {
        for (PointedDripstone.Thickness thickness : PointedDripstone.Thickness.values()) {
            AtomicReference<Object> written = new AtomicReference<>();
            PointedDripstone dripstone = (PointedDripstone) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { PointedDripstone.class },
                    (proxy, method, arguments) -> {
                        if (method.getName().equals("getThickness")) return thickness;
                        if (method.getName().equals("setThickness")) written.set(arguments[0]);
                        return null;
                    });
            DripstoneThickness common = DripstoneThickness.valueOf(thickness.name());
            assertEquals(common, adapter.getDripstoneThickness(dripstone));
            adapter.setDripstoneThickness(dripstone, common);
            assertEquals(thickness, written.get());
        }
    }

    @Test public void sendRetainsItemHover() {
        ItemMeta meta = (ItemMeta) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { ItemMeta.class },
                (proxy, method, arguments) -> method.getName().equals("getAsString") ? "{}" : null);
        ItemStack stack = new ItemStack() {
            @Override public Material getType() { return Material.STONE; }
            @Override public ItemMeta getItemMeta() { return meta; }
        };
        AtomicReference<BaseComponent> sent = new AtomicReference<>();
        CommandSender.Spigot sender = new CommandSender.Spigot() {
            @Override public void sendMessage(BaseComponent component) { sent.set(component); }
        };
        CommandSender target = (CommandSender) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { CommandSender.class },
                (proxy, method, arguments) -> method.getName().equals("spigot") ? sender : null);
        adapter.sendComponent(target, Components.text("item").hover(Hover.item(stack)));
        Item item = (Item) sent.get().getHoverEvent().getContents().getFirst();
        assertEquals("minecraft:stone", item.getId());
        assertEquals(1, item.getCount());
    }
}
