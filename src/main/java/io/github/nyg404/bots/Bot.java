package io.github.nyg404.bots;

import io.github.nyg404.bots.commands.TestCommand;
import io.github.nyg404.db.DataConnection;
import io.github.nyg404.handler.HandlerCommand;
import io.github.nyg404.task.MessageDispetcher;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;

public class Bot implements LongPollingSingleThreadUpdateConsumer {
    private final MessageDispetcher dispatcher = new MessageDispetcher(5);
    public Bot() {
        dispatcher.start();
        HandlerCommand.registerCommand(new TestCommand());
    }
    @Override
    public void consume(Update update) {
        HandlerCommand.update(update, dispatcher);
    }
}
