package io.github.nyg404.bots.commands;


import io.github.nyg404.service.DataService;
import io.github.nyg404.handler.ICommand;
import io.github.nyg404.models.UserStats;
import io.github.nyg404.task.MessageDispetcher;
import io.github.nyg404.task.TaskDispatcher;
import org.telegram.telegrambots.meta.api.methods.GetUserProfilePhotos;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.photo.PhotoSize;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.InputStream;

public class BioCommand implements ICommand {
    private final TaskDispatcher taskDispatcher;
    private final TelegramClient telegramClient;
    private static final DataService dm = DataService.getInstance();

    public BioCommand(TaskDispatcher taskDispatcher, TelegramClient telegramClient) {
        this.taskDispatcher = taskDispatcher;
        this.telegramClient = telegramClient;
    }

    @Override
    public String name() {
        return "bio";
    }

    @Override
    public String prefix() {
        return "/";
    }

    @Override
    public void update(Update update, MessageDispetcher dispatcher) {
        String text = update.getMessage().getText();
        long userId = update.getMessage().getFrom().getId();
        User user = update.getMessage().getFrom();
        String chatId = update.getMessage().getChatId().toString();
        String rest = text.substring(1);
        String[] parts = rest.split("\\s+", 3); // Split into at most 3 parts
        UserStats stats = dm.getUserGroupStats(userId, chatId);

        if (parts.length == 1) {
            // No arguments, show the user's bio
            String bio = stats != null && stats.bio() != null ? stats.bio() : "Биография не установлена. Для установки используйте команды /bio add|edit текст";
            dispatcher.add(chatId, () -> {
                try {
                    sendPhoto(update, getAvatar(userId), "Пользователь! Вот ваша карточка бота. \n" + "Ваше имя: " + user.getFirstName() + "\n" + "Ваше айди: " + user.getId() + "\n" + "Ваша биография: " + bio );
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            });
            return;
        }

        String arg = parts[1].toLowerCase();
        String textBio = parts.length > 2 ? parts[2] : null;

        switch (arg) {
            case "add": {
                if (textBio == null) {
                    dispatcher.add(chatId, () -> sendMessage(update, "Пожалуйста, укажите текст биографии."));
                    return;
                }
                taskDispatcher.submitTask(() -> {
                    dm.updateUserBio(userId, chatId, textBio);
                    dispatcher.add(chatId, () -> sendMessage(update, "Пользователь: " + userId + " Вы успешно добавили свою биографию!\n" + textBio));
                });
                break;
            }
            case "edit": {
                if (textBio == null) {
                    dispatcher.add(chatId, () -> sendMessage(update, "Пожалуйста, укажите текст биографии."));
                    return;
                }
                taskDispatcher.submitTask(() -> {
                    dm.updateUserBio(userId, chatId, textBio);
                    dispatcher.add(chatId, () -> sendMessage(update, "Пользователь: " + userId + " Вы успешно изменили свою биографию!\n" + textBio));
                });
                break;
            }
            case "remove": {
                taskDispatcher.submitTask(() -> {
                    dm.removeUserBio(userId, chatId);
                    dispatcher.add(chatId, () -> sendMessage(update, "Пользователь: " + userId + " Ваша биография успешно удалена."));
                });
                break;
            }
            default: {
                dispatcher.add(chatId, () -> sendMessage(update, "Неизвестное действие. Используйте add, remove или edit."));
                break;
            }
        }
    }

    private InputFile getAvatar(long userId) throws TelegramApiException {
        GetUserProfilePhotos getPhotos = new GetUserProfilePhotos(userId);
        getPhotos.setLimit(1);

        UserProfilePhotos photos = telegramClient.execute(getPhotos);

        if (photos.getTotalCount() > 0) {
            PhotoSize photo = photos.getPhotos().get(0)
                    .get(photos.getPhotos().get(0).size() - 1);
            String fileId = photo.getFileId();

            return new InputFile(fileId);
        }

        InputStream defaultAvatar = getClass().getResourceAsStream("/photo/NotFountAvatar.jpg");
        return new InputFile(defaultAvatar, "NotFountAvatar.jpg");
    }


    private void sendMessage(Update update, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(update.getMessage().getChatId())
                .text(text)
                .build();
        try {
            telegramClient.executeAsync(message);
        } catch (TelegramApiException e) {
            // Log the exception if needed
        }
    }

    private void sendPhoto(Update update, InputFile file, String text){
        SendPhoto sendPhoto = SendPhoto.builder()
                .chatId(update.getMessage().getChatId())
                .photo(file)
                .caption(text)
                .build();
        telegramClient.executeAsync(sendPhoto);
    }
}