package io.github.nyg404.task;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

@Slf4j
public class MessageDispetcher {
    private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final long delayMs;

    public MessageDispetcher(int maxMessagesPerSecond) {
        this.delayMs = 1000L / maxMessagesPerSecond;
    }

    public void start() {
        executor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Runnable task = queue.take(); // блокировка до следующей задачи
                    log.info("В очереди осталось: {}", queue.size()); // сразу после take()
                    task.run(); // выполняем задачу
                    Thread.sleep(delayMs); // задержка между задачами
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("Ошибка при выполнении задачи", e);
                }
            }
        });
    }


    public void add(Runnable task) {
        queue.add(task);
        log.info("Добавлена задача в очередь. Размер очереди: {}", queue.size());
    }

    public void stop() {
        executor.shutdownNow();
    }

    public int getQueueSize() {
        return queue.size();
    }
}
