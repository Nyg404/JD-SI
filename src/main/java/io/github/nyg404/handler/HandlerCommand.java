package io.github.nyg404.handler;

import io.github.nyg404.task.MessageDispetcher;
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

    public static void update(Update update, MessageDispetcher dispatcher){
        String text = update.getMessage().getText().trim();

        for (ICommand cmd : commands) {
            if (!text.startsWith(cmd.prefix())) continue; // пропускаем неподходящие префиксы

            String commandName = text.substring(cmd.prefix().length()).split(" ")[0].toLowerCase();
            if (cmd.name().toLowerCase().equals(commandName)) {
                executor.submit(() -> cmd.update(update, dispatcher));
                break; // нашли команду, больше не ищем
            }
        }

    }

    public static void shutdown(){
        executor.shutdown();
    }
}
