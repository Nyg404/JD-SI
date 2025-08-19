package io.github.nyg404.core.task;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class MessageDispetcher {
    private final BlockingQueue<ChatTask> chatTasks = new LinkedBlockingQueue<>();
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    private final int tokenCapacity;
    private final long refillIntervalMs;

    private final ExecutorService taskExecutor;  // пул для выполнения задач
    private final Thread dispatcherThread;       // поток обработки очереди


    public MessageDispetcher(int tokenCapacity, long refillIntervalMs, int taskPoolSize) {
        this.tokenCapacity = tokenCapacity;
        this.refillIntervalMs = refillIntervalMs;

        this.taskExecutor = Executors.newFixedThreadPool(taskPoolSize, r -> {
            Thread t = new Thread(r);
            t.setName("message-task-executor-" + System.nanoTime());
            return t;
        });

        this.dispatcherThread = new Thread(this::processQueue, "message-dispatcher-thread");
    }

    private void processQueue() {
        while (isRunning.get() && !Thread.currentThread().isInterrupted()) {
            try {
                ChatTask chatTask = chatTasks.poll(100, TimeUnit.MILLISECONDS);
                if (chatTask == null) continue;

                TokenBucket tokenBucket = buckets.computeIfAbsent(chatTask.chatId(),
                        id -> new TokenBucket(tokenCapacity, refillIntervalMs));

                if (tokenBucket.tryConsume()) {
                    taskExecutor.submit(chatTask.task()); // выполняем в пуле задач
                } else {
                    chatTasks.add(chatTask); // возвращаем обратно
                    Thread.sleep(tokenBucket.refillIntervalMs);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Ошибка при обработке задачи: {}", e.getMessage(), e);
            }
        }
    }

    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            dispatcherThread.start();
            log.info("MessageDispetcher запущен: capacity={}, refillIntervalMs={}, taskPoolSize={}",
                    tokenCapacity, refillIntervalMs, ((ThreadPoolExecutor) taskExecutor).getCorePoolSize());
        }
    }

    public void stop() {
        isRunning.set(false);
        dispatcherThread.interrupt();
        taskExecutor.shutdown();
        try {
            if (!taskExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                taskExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            taskExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        buckets.values().forEach(TokenBucket::shutdown);
        log.info("MessageDispetcher остановлен");
    }

    public void add(String chatId, Runnable task) {
        if (chatId == null || task == null)
            throw new IllegalArgumentException("chatId и task не могут быть null");
        chatTasks.add(new ChatTask(chatId, task));
        log.info("Добавлена задача для chatId={}. Очередь: {}", chatId, chatTasks.size());
    }

    public int getQueueSize() {
        return chatTasks.size();
    }

    public int getTokens(String chatId) {
        if (chatId == null) return 0;
        TokenBucket bucket = buckets.computeIfAbsent(chatId, id -> new TokenBucket(tokenCapacity, refillIntervalMs));
        return bucket.getTokens();
    }

    private record ChatTask(String chatId, Runnable task) {}

    private static class TokenBucket {
        private final int capacity;
        private final long refillIntervalMs;
        private int tokens;
        private final ScheduledExecutorService scheduler;
        private long lastRefillTime;
        public TokenBucket(int capacity, long refillIntervalMs) {
            this.capacity = capacity;
            this.tokens = capacity;
            this.refillIntervalMs = refillIntervalMs;
            this.lastRefillTime = System.currentTimeMillis();
            this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r);
                t.setName("token-bucket-refill-" + System.nanoTime());
                return t;
            });
            scheduler.scheduleAtFixedRate(this::refill, refillIntervalMs, refillIntervalMs, TimeUnit.MILLISECONDS);
        }

        public synchronized boolean tryConsume() {
            if (tokens > 0) {
                tokens--;
                return true;
            }
            return false;
        }

        public synchronized int getTokens() {
            return tokens;
        }

        private synchronized void refill() {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRefillTime;
            int newTokens = (int) (elapsed / refillIntervalMs);
            if (newTokens > 0) {
                tokens = Math.min(capacity, tokens + newTokens);
                lastRefillTime += newTokens * refillIntervalMs;
            }
        }


        public void shutdown() {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
