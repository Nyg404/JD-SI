package io.github.nyg404.configuration;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.Getter;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Getter
public class BotConfiguration {
    private final String token;
    private final TelegramClient client;

    public BotConfiguration() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        this.token = dotenv.get("TOKEN");
        if (this.token == null || this.token.isEmpty()) {
            throw new IllegalStateException("Токен Telegram не найден в .env файле");
        }
        try {
            this.client = new OkHttpTelegramClient(token);
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось создать TelegramClient: " + e.getMessage(), e);
        }
    }
}