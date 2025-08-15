package io.github.nyg404.task;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
public class MessageDispetcher {
    private final BlockingQueue<ChatTask> queue = new LinkedBlockingQueue<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public void start(){
        executor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()){
                try {
                    ChatTask chatTask = queue.take();
                    TokenBucket tokenBucket = buckets.computeIfAbsent(chatTask.chatId, id -> new TokenBucket(28, 2143));

                    log.info("Обрабатываем задачу для chatId={} | Токенов в бакете: {}", chatTask.chatId, tokenBucket.tokens);

                    if(tokenBucket.tryConsume()){
                        log.info("Токен использован, выполняем задачу для chatId={}", chatTask.chatId);
                        chatTask.task().run();
                    } else {
                        log.info("Нет токенов для chatId={}, возвращаем задачу в очередь", chatTask.chatId);
                        queue.put(chatTask);
                        Thread.sleep(tokenBucket.refillIntervalMs);
                    }
                }catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("Ошибка при выполнении задачи", e);
                }
            }
        });
    }

    public int getTokens(String chatId) {
        TokenBucket bucket = buckets.get(chatId);
        return bucket != null ? bucket.tokens : 0;
    }


    public void stop() {
        executor.shutdownNow();
    }

    public int getQueueSize() {
        return queue.size();
    }
    public void add(String chatId, Runnable task) {
        queue.add(new ChatTask(chatId, task));
        log.info("Добавлена задача в очередь. Размер очереди: {}", queue.size());
    }

    private record ChatTask(String chatId, Runnable task){}


    private static class TokenBucket {
        private final int capacity;
        private final long refillIntervalMs;
        private int tokens;
        private long lastRefillTime;

        public TokenBucket(int capacity, long refillIntervalMs) {
            this.capacity = capacity;
            this.tokens = capacity;
            this.refillIntervalMs = refillIntervalMs;
            this.lastRefillTime = System.currentTimeMillis();
        }

        public synchronized boolean tryConsume() {
            refill();
            if (tokens > 0) {
                tokens--;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRefillTime;
            int newTokens = (int)(elapsed / refillIntervalMs);
            if (newTokens > 0) {
                tokens = Math.min(capacity, tokens + newTokens);
                log.info("Пополнение токенов для бакета: tokens={}, capacity={}", tokens, capacity);
                lastRefillTime = now;
            }
        }


        public long refillIntervalMs() {
            return refillIntervalMs;
        }
    }
}
