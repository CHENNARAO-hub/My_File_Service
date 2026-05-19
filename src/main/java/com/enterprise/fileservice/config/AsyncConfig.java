package com.enterprise.fileservice.config;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

	@Bean(name = "auditExecutor")
	public Executor auditExecutor() {

	    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
	    executor.setCorePoolSize(5);
	    executor.setMaxPoolSize(10);
	    executor.setQueueCapacity(100);
	    executor.initialize();

	    return new DelegatingSecurityContextAsyncTaskExecutor(executor);
	}
    // 🔥 STEP 5 (ADD HERE)
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            System.err.println("Async error in method: " + method.getName());
            System.err.println("Error: " + ex.getMessage());
        };
    }

}