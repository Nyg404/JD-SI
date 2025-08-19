package io.github.nyg404.core.service;

import io.github.nyg404.config.BotConfiguration;
import io.github.nyg404.core.dao.DataConnection;
import io.github.nyg404.core.task.MessageDispetcher;
import io.github.nyg404.core.task.TaskDispatcher;
import lombok.Getter;

@Getter
public class ServiceManager {
    private final MessageDispetcher messageDispatcher;
    private final TaskDispatcher taskDispatcher;
    private final BotConfiguration config;

    public ServiceManager(BotConfiguration config) {
        this.config = config;
        this.messageDispatcher = new MessageDispetcher(25, 2414, 2);
        this.taskDispatcher = new TaskDispatcher(messageDispatcher);
    }

    public void start() {
        DataConnection.getInstance().createDefaultTable(); // Временное решение, см. ниже
        messageDispatcher.start();
    }

    public void stop() {
        messageDispatcher.stop();
    }
}