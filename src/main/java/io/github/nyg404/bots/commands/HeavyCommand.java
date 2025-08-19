package io.github.nyg404.bots.commands;

import io.github.nyg404.handler.ICommand;

import io.github.nyg404.core.task.MessageDispetcher;
import io.github.nyg404.core.task.TaskDispatcher;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * Команда /heavy, возвращающая количество оставшихся токенов для чата.
 */
@Slf4j
public class HeavyCommand implements ICommand {
    private final TaskDispatcher taskDispatcher;
    private final TelegramClient telegramClient;

    public HeavyCommand(TaskDispatcher taskDispatcher, TelegramClient telegramClient) {
        this.taskDispatcher = taskDispatcher;
        this.telegramClient = telegramClient;
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
        if (update == null || update.getMessage() == null || update.getMessage().getChatId() == null) {
            log.warn("Получен некорректный Update: {}", update);
            return;
        }

        taskDispatcher.submitTask(() -> {
            String chatId = update.getMessage().getChatId().toString();
            String result = "Токенов осталось: " + dispatcher.getTokens(chatId);
            log.debug("Подготовлено сообщение для chatId={}: {}", chatId, result);
            dispatcher.add(chatId, () -> sendMessage(update, result));
        });
    }

    private void sendMessage(Update update, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(update.getMessage().getChatId())
                .text(text)
                .build();
        try {
            telegramClient.execute(message);
            log.info("Сообщение отправлено в чат {}: {}", update.getMessage().getChatId(), text);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения в чат {}: {}", update.getMessage().getChatId(), e.getMessage(), e);
            if (e.getMessage().contains("429")) {
                log.warn("Получена ошибка 429 Too Many Requests, требуется замедление");
            }
        }
    }
}