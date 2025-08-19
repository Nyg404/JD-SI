package io.github.nyg404.handler;

import io.github.nyg404.core.task.MessageDispetcher;
import org.telegram.telegrambots.meta.api.objects.Update;

public interface ICommand {
    String name();
    String prefix();
    void update(Update update, MessageDispetcher dispatcher);



}
