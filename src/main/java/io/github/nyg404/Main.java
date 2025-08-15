package io.github.nyg404;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.nyg404.bots.Bot;
import io.github.nyg404.db.DataConnection;
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
    public static void main(String[] args) {
        DataConnection.createDefaultTable();
        try {
            TelegramBotsLongPollingApplication application = new TelegramBotsLongPollingApplication();
            application.registerBot(token, new Bot());
            log.info("Бот запустился.");
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}