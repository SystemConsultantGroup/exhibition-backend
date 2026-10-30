package kr.ac.skku.scg.exhibition.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class MediaUploadConfigTest {
    /** 큐가 포화되어도 요청 스레드에서 I/O를 실행해 작업자 상한을 넘지 않는다. */
    @Test
    void saturationAppliesBackpressureWithoutExceedingWorkerLimit() {
        var executor = new MediaUploadConfig().mediaUploadExecutor(2);
        executor.initialize();
        var active = new AtomicInteger();
        var maximum = new AtomicInteger();
        var completed = new AtomicInteger();
        Thread caller = Thread.currentThread();
        try {
            var tasks = new ArrayList<CompletableFuture<Void>>();
            for (int i = 0; i < 40; i++) {
                tasks.add(CompletableFuture.runAsync(() -> {
                    assertThat(Thread.currentThread()).isNotSameAs(caller);
                    maximum.accumulateAndGet(active.incrementAndGet(), Math::max);
                    try { Thread.sleep(2); }
                    catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
                    finally { active.decrementAndGet(); completed.incrementAndGet(); }
                }, executor));
            }
            CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new)).join();
            assertThat(maximum.get()).isEqualTo(2);
            assertThat(completed.get()).isEqualTo(40);
        } finally { executor.shutdown(); }
    }

    @Test
    void rejectsInvalidWorkerSettings() {
        for (int workers : new int[]{0, -1, 33}) {
            assertThatThrownBy(() -> new MediaUploadConfig().mediaUploadExecutor(workers))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
