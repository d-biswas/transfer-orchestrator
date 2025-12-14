package com.company.orchestrator.infrastructure.config;

import com.company.orchestrator.infrastructure.props.EdcProperties;
import jakarta.annotation.Nonnull;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
@Configuration
public class EdcConfig {

    /**
     * ScheduledExecutorService for delayed async callbacks
     * Simulates EDC's async behavior with realistic timing
     */
    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService mockEdcScheduler(EdcProperties properties) {
        log.info("Creating ScheduledExecutorService for Mock EDC with pool size: {}",
                properties.getSchedulerThreadPoolSize());

        return Executors.newScheduledThreadPool(
                properties.getSchedulerThreadPoolSize(),
                new MockEdcThreadFactory()
        );
    }

    /**
     * Custom thread factory for Mock EDC scheduler threads
     */
    private static class MockEdcThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(@Nonnull Runnable r) {
            Thread thread = new Thread(r, "mock-edc-" + threadNumber.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
