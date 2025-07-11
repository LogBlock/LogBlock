package de.diddiz.LogBlock.componentwrapper;

public class TextHover implements Hover {
    private final Component text;

    TextHover(Component text) {
        this.text = text;
    }

    public Component getText() {
        return text;
    }
}
