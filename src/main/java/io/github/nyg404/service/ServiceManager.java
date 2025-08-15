package io.github.nyg404.service;

import io.github.nyg404.db.DataConnection;
import io.github.nyg404.task.MessageDispetcher;
import io.github.nyg404.task.TaskDispatcher;

public class ServiceManager {
    private final MessageDispetcher messageDispetcher;
    private final TaskDispatcher taskDispatcher;

    public ServiceManager() {
        this.messageDispetcher = new MessageDispetcher();
        this.taskDispatcher = new TaskDispatcher(messageDispetcher);
    }

    public MessageDispetcher getMessageDispetcher() {
        return messageDispetcher;
    }

    public TaskDispatcher getTaskDispatcher() {
        return taskDispatcher;
    }

    public void start() {
        DataConnection.createDefaultTable();
        messageDispetcher.start();
    }

    public void stop() {
        messageDispetcher.stop();
    }
}
