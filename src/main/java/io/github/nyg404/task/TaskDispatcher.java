package io.github.nyg404.task;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

@Slf4j

public class TaskDispatcher {
    private final ThreadPoolExecutor request;
    private final MessageDispetcher messageDispetcher;

    public TaskDispatcher(MessageDispetcher messageDispetcher) {
        this.messageDispetcher = messageDispetcher;
        this.request = new ThreadPoolExecutor(
                4, 12, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>()
        );
    }

    public void submitTask(Runnable task){
        log.info("Добавилась новая задача. Активные потоки: {}, Очередь: {}",
                request.getActiveCount(), request.getQueue().size());
        request.submit(() -> {
            try {
                task.run();
            } catch (Exception e) {
                log.error("Ошибка в тяжелой задаче", e);
            }
        });
    }

}
