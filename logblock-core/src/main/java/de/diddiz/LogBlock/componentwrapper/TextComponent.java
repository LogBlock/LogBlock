package de.diddiz.LogBlock.componentwrapper;

import java.util.List;

public class TextComponent extends Component {
    private final String text;

    TextComponent(String text, ChatColor color, Hover hover, Click click, Component... children) {
        super(color, hover, click, children);
        this.text = text;
    }

    TextComponent(String text, ChatColor color, Hover hover, Click click, List<Component> children) {
        super(color, hover, click, children);
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public TextComponent append(String child) {
        return append(Components.text(child));
    }

    @Override
    public TextComponent append(Component child) {
        return Components.text(text, getColor(), getHover(), getClick(), addChildInternal(child));
    }

    @Override
    public TextComponent hover(Hover hover) {
        return new TextComponent(text, getColor(), hover, getClick(), getChildren());
    }

    @Override
    public TextComponent click(Click click) {
        return new TextComponent(text, getColor(), getHover(), click, getChildren());
    }
}
