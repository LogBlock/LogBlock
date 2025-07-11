package de.diddiz.LogBlock.componentwrapper;

import java.util.List;

public abstract class Component {
    private final ChatColor color;
    private final Hover hover;
    private final Click click;
    private final List<Component> children;

    Component(ChatColor color, Hover hover, Click click, Component... children) {
        this(color, hover, click, List.of(children));
        for (Component e : this.children) {
            if (e == null) {
                throw new NullPointerException("no null children allowed");
            }
        }
    }

    Component(ChatColor color, Hover hover, Click click, List<Component> children) {
        this.color = color;
        this.hover = hover;
        this.click = click;
        this.children = children;
    }

    public ChatColor getColor() {
        return color;
    }

    public List<Component> getChildren() {
        return children;
    }

    public Hover getHover() {
        return hover;
    }

    public Click getClick() {
        return click;
    }

    public abstract Component append(Component child);

    public abstract Component append(String text);

    public abstract Component hover(Hover hover);

    public abstract Component click(Click click);

    protected Component[] addChildInternal(Component child) {
        if (child == null) {
            throw new NullPointerException("no null children allowed");
        }
        if (children.isEmpty()) {
            return new Component[] { child };
        }
        int size = children.size();
        Component[] array = children.toArray(new Component[size + 1]);
        array[size] = child;
        return array;
    }
}
