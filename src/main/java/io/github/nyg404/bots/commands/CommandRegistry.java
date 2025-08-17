package io.github.nyg404.bots.commands;

import io.github.nyg404.handler.ICommand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandRegistry {
    private final List<ICommand> commands = new ArrayList<>();

    public void register(ICommand command) {
        commands.add(command);
    }

    public List<ICommand> getCommands() {
        return Collections.unmodifiableList(commands);
    }
}