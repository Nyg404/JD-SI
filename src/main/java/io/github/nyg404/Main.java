package io.github.nyg404;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.nyg404.bots.Bot;
import io.github.nyg404.db.DataConnection;
import io.github.nyg404.service.ServiceManager;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
public class Main {
    public static final Dotenv dotenv = Dotenv.load();
    public static final String token = dotenv.get("TOKEN");
    public static final TelegramClient CLIENT = new OkHttpTelegramClient(token);
    public static final ServiceManager service = new ServiceManager();
    public static void main(String[] args) {
        try {
            service.start();
            TelegramBotsLongPollingApplication application = new TelegramBotsLongPollingApplication();
            application.registerBot(token, new Bot(service.getMessageDispetcher(), service.getTaskDispatcher()));
            log.info("Бот запустился.");
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}