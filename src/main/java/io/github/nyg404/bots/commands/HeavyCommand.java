package io.github.nyg404.bots.commands;

import io.github.nyg404.Main;
import io.github.nyg404.handler.ICommand;
import io.github.nyg404.task.MessageDispetcher;
import io.github.nyg404.task.TaskDispatcher;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class HeavyCommand implements ICommand {
    private final TaskDispatcher taskDispatcher;

    public HeavyCommand(TaskDispatcher taskDispatcher) {
        this.taskDispatcher = taskDispatcher;
    }

    @Override
    public String name() {
        return "heavy";
    }

    @Override
    public String prefix() {
        return "/";
    }

    @Override
    public void update(Update update, MessageDispetcher dispatcher) {
        // Отправляем задачу в TaskDispatcher (параллельно)

        taskDispatcher.submitTask(() -> {
            String result = "Токенов осталось: " + dispatcher.getTokens(update.getMessage().getChatId().toString());
            dispatcher.add(update.getMessage().getChatId().toString(), () -> sendMessage(update, result));

        });
    }


    private void sendMessage(Update update, String text) {
        SendMessage.SendMessageBuilder<?, ?> msg = SendMessage.builder();
        msg.chatId(update.getMessage().getChatId());
        msg.text(text);
        try {
            // Здесь желательно не использовать Main.CLIENT напрямую, а передавать клиент через конструктор
            Main.CLIENT.execute(msg.build());
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}
