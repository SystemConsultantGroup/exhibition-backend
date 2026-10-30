package kr.ac.skku.scg.exhibition.global.config;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class MediaUploadConfig {
    /** 전체 요청이 공유하는 유한 풀로 파일/소켓 수를 제한하고 생산자에게 역압을 준다. */
    @Bean
    public ThreadPoolTaskExecutor mediaUploadExecutor(@Value("${app.upload.workers:4}") int workers) {
        if (workers < 1 || workers > 32) throw new IllegalArgumentException("app.upload.workers must be between 1 and 32");
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(workers);
        executor.setMaxPoolSize(workers);
        executor.setQueueCapacity(workers * 2);
        executor.setThreadNamePrefix("media-upload-");
        executor.setRejectedExecutionHandler((task, pool) -> {
            // CallerRuns는 동시 요청 수만큼 I/O 동시성을 늘리므로 큐에 역압을 건다.
            while (!pool.isShutdown()) {
                try {
                    if (pool.getQueue().offer(task, 1, TimeUnit.SECONDS)) return;
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new RejectedExecutionException("Interrupted while waiting for media upload capacity", ex);
                }
            }
            throw new RejectedExecutionException("Media upload executor is shutting down");
        });
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        return executor;
    }
}
