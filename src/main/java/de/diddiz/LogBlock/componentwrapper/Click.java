package de.diddiz.LogBlock.componentwrapper;

public interface Click {
    public static RunCommandClick run(String command) {
        return new RunCommandClick(command);
    }
}
