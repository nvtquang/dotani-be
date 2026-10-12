package com.dotani.shared.config;

import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @Async work (notifications) runs on a small thread pool; @Scheduled jobs (deadline reminders) are enabled.
 * Tests set app.async.enabled=false so listeners run inline and results can be asserted immediately.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncSchedulingConfig implements AsyncConfigurer {

    private final boolean asyncEnabled;

    public AsyncSchedulingConfig(@Value("${app.async.enabled:true}") boolean asyncEnabled) {
        this.asyncEnabled = asyncEnabled;
    }

    @Override
    public Executor getAsyncExecutor() {
        if (!asyncEnabled) {
            return new SyncTaskExecutor();
        }
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("dotani-async-");
        executor.initialize();
        return executor;
    }
}
