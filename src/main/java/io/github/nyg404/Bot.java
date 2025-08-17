package io.github.nyg404;

import io.github.nyg404.bots.commands.BioCommand;
import io.github.nyg404.bots.commands.HandlerCommand;
import io.github.nyg404.bots.commands.HeavyCommand;
import io.github.nyg404.handler.Event;
import io.github.nyg404.task.MessageDispetcher;
import io.github.nyg404.task.TaskDispatcher;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
@Slf4j
public class Bot implements LongPollingSingleThreadUpdateConsumer {
    private final MessageDispetcher dispatcher;
    private final TaskDispatcher taskDispatcher;
    private final TelegramClient client;

    public Bot(MessageDispetcher dispatcher, TaskDispatcher taskDispatcher, TelegramClient client) {
        this.dispatcher = dispatcher;
        this.taskDispatcher = taskDispatcher;
        this.client = client;
        HandlerCommand.registerCommand(new HeavyCommand(taskDispatcher, client), new BioCommand(taskDispatcher, client));
    }

    public long getBotId() {
        try {
            return client.execute(new GetMe()).getId();
        } catch (TelegramApiException e) {
            log.error("Ошибка при получении ID бота: {}", e.getMessage(), e);
            return -1;
        }
    }

    @Override
    public void consume(Update update) {
        HandlerCommand.update(update, dispatcher);
        Event event = new Event(update, getBotId());
        event.execute();
    }
}