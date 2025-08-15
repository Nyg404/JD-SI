package io.github.nyg404.bots.commands;

import io.github.nyg404.Main;
import io.github.nyg404.handler.ICommand;
import io.github.nyg404.task.MessageDispetcher;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class TestCommand implements ICommand {
    @Override
    public String name() {
        return "t";
    }

    @Override
    public String prefix() {
        return "/";
    }


    @Override
    public void update(Update update, MessageDispetcher dispatcher) {
        // пример тяжелой работы
        String result = heavyComputation(update.getMessage().getText());

        // отправка результата в очередь сообщений
        dispatcher.add(() -> sendMessage(update, result));
    }

    private String heavyComputation(String input){
        try {
            Thread.sleep(2000); // имитация тяжелой работы
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "Processed: " + input;
    }

    private void sendMessage(Update update, String text){
        SendMessage.SendMessageBuilder<?,?> msg = SendMessage.builder();
        msg.chatId(update.getMessage().getChatId());
        msg.text(text);
        try {
            Main.CLIENT.execute(msg.build());
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }


}
