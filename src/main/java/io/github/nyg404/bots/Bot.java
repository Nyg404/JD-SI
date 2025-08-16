package io.github.nyg404.bots;

import io.github.nyg404.Main;
import io.github.nyg404.bots.commands.HeavyCommand;

import io.github.nyg404.handler.Event;
import io.github.nyg404.handler.HandlerCommand;
import io.github.nyg404.service.ServiceManager;
import io.github.nyg404.task.MessageDispetcher;
import io.github.nyg404.task.TaskDispatcher;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class Bot implements LongPollingSingleThreadUpdateConsumer {
    private final MessageDispetcher dispatcher;
    private final TaskDispatcher taskDispatcher;

    public Bot(MessageDispetcher dispatcher, TaskDispatcher taskDispatcher) {
        this.dispatcher = dispatcher;
        this.taskDispatcher = taskDispatcher;
        HandlerCommand.registerCommand(new HeavyCommand(taskDispatcher));
    }

    public long getBotId() {
        try {
            return Main.CLIENT.execute(new GetMe()).getId();
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return -1;
        }
    }

    @Override
    public void consume(Update update) {
        HandlerCommand.update(update, dispatcher);
        Event event = new Event(update, 7923116742L);
        event.execute();
    }
}

