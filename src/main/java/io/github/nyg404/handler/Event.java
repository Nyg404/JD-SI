package io.github.nyg404.handler;

import io.github.nyg404.core.service.DataService;
import io.github.nyg404.core.models.UserKey;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberUpdated;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class Event {
    private static final DataService dm = DataService.getInstance();
    private final Update update;
    private final long botId;
    private final Object lock = new Object();

    public Event(Update update, long botId) {
        this.update = update;
        this.botId = botId;
    }

    public void execute() {
        // Обрабатываем апдейт безопасно
        if (update.hasMessage() && update.getMessage().getChat() != null && update.getMessage().getFrom() != null) {
            UserKey key = extract(update);
            String chatType = update.getMessage().getChat().getType();
            Chat chat = new Chat(update.getMessage().getChatId(), chatType);

            // Обработка событий вступления бота в группу
            if(update.hasChatMember()) {
                if(isBotFirstGroup(update.getChatMember())) {
                    if(groupFirst(chat.getId().toString())) {
                        dm.addGroup(chat.getId().toString(), true);
                    }
                }
            }

            switch (chatType) {
                case "private" -> handlePrivate(key);
                case "group", "supergroup" -> handleGroup(key);
                default -> log.info("Неизвестный тип чата: {}", chatType);
            }
        } else if (update.hasMyChatMember()) {
            // Если апдейт типа my_chat_member (например, бот добавлен в группу)
            ChatMemberUpdated memberUpdated = update.getMyChatMember();
            long newUserId = memberUpdated.getNewChatMember().getUser().getId();
            String chatId = memberUpdated.getChat().getId().toString();

            if(newUserId == botId && groupFirst(chatId)) {
                dm.addGroup(chatId, true);
            }
        } else {
            log.info("Пропущен апдейт, не поддерживаемый Event: {}", update);
        }
    }

    private void handlePrivate(UserKey key) {
        if (userFirstJoin(key)) {
            dm.addUser(key.userId(), true);
        }
    }

    private void handleGroup(UserKey key) {
        synchronized (getUserLock(key.userId())){
            if (userFirstJoin(key)) {
                dm.addUser(key.userId(), false);
            }
        }
        if (userFirstJoinGroup(key)) {
            dm.addUserToGroup(key.userId(), key.groupId(), "None");
        }
    }

    private UserKey extract(Update update){
        long userId = update.getMessage().getFrom().getId();
        String chatId = update.getMessage().getChatId().toString();
        return new UserKey(userId, chatId);
    }

    private boolean userFirstJoin(UserKey key){
        return !dm.getUserById(key.userId());
    }

    private boolean userFirstJoinGroup(UserKey key){
        return dm.getUserGroupStats(key.userId(), key.groupId()) == null;
    }

    private boolean groupFirst(String chatId){
        return !dm.getGroupById(chatId);
    }

    private boolean isBotFirstGroup(ChatMemberUpdated chatMemberUpdated){
        long newUserId = chatMemberUpdated.getNewChatMember().getUser().getId();
        String chatId = chatMemberUpdated.getChat().getId().toString();
        return newUserId == botId && !dm.getGroupById(chatId);
    }

    private static final Map<Long, Object> userLocks = new ConcurrentHashMap<>();

    private static Object getUserLock(long userId) {
        return userLocks.computeIfAbsent(userId, k -> new Object());
    }
}
