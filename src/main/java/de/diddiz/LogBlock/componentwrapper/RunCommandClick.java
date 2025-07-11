package de.diddiz.LogBlock.componentwrapper;

public class RunCommandClick implements Click {
    private final String command;

    public RunCommandClick(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }
}
