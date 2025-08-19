package io.github.nyg404;

import io.github.nyg404.config.BotConfiguration;
import io.github.nyg404.core.service.ServiceManager;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
public class Main {
    private static ServiceManager serviceManager;

    public static void main(String[] args) {
        try {
            Bot bot = initializeBot();
            log.info("Бот запустился с ID: {}", bot.getBotId());
            // Добавляем обработку завершения работы
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("Остановка бота...");
                if (serviceManager != null) {
                    serviceManager.stop();
                }
            }));
        } catch (TelegramApiException e) {
            log.error("Ошибка при запуске бота: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось запустить бота", e);
        }
    }

    @NotNull
    private static Bot initializeBot() throws TelegramApiException {
        BotConfiguration config = new BotConfiguration();
        serviceManager = new ServiceManager(config);
        serviceManager.start();
        Bot bot = new Bot(serviceManager.getMessageDispatcher(), serviceManager.getTaskDispatcher(), config.getClient());
        registerBot(config, bot);
        return bot;
    }

    private static void registerBot(BotConfiguration config, Bot bot) throws TelegramApiException {
        TelegramBotsLongPollingApplication application = new TelegramBotsLongPollingApplication();
        application.registerBot(config.getToken(), bot);
    }
}