package io.github.nyg404.task;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.function.Supplier;

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


    public <T> CompletableFuture<T> submitTaskAsync(Supplier<T> task) {
        log.info("Добавилась новая задача. Активные потоки: {}, Очередь: {}",
                request.getActiveCount(), request.getQueue().size());

        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.get();
            } catch (Exception e) {
                log.error("Ошибка в тяжелой задаче", e);
                throw new RuntimeException(e);
            }
        }, request); // request — твой ExecutorService
    }

}
