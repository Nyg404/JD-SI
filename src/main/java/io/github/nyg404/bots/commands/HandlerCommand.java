package io.github.nyg404.bots.commands;

import io.github.nyg404.handler.ICommand;
import io.github.nyg404.core.task.MessageDispetcher;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class HandlerCommand {
    private static final List<ICommand> commands = new ArrayList<>();
    // пул потоков для параллельной обработки команд
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);



    public static void registerCommand(ICommand... icommands){
        Collections.addAll(commands, icommands);
    }

    private static final ExecutorService commandExecutor = Executors.newFixedThreadPool(10);

    public static void update(Update update, MessageDispetcher dispatcher){
        commandExecutor.submit(() -> {
            String text = update.getMessage().getText().trim();
            for (ICommand cmd : commands) {
                if (!text.startsWith(cmd.prefix())) continue;
                String commandName = text.substring(cmd.prefix().length()).split(" ")[0].toLowerCase();
                if (cmd.name().toLowerCase().equals(commandName)) {
                    cmd.update(update, dispatcher); // сюда можно передавать taskDispatcher
                    break;
                }
            }
        });
    }


    public static void shutdown(){
        commandExecutor.shutdown();
    }
}
