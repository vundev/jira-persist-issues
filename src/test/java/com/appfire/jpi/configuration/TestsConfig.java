package com.appfire.jpi.configuration;

import java.util.concurrent.ExecutorService;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.appfire.jpi.utils.SameThreadExecutorService;

@TestConfiguration
public class TestsConfig {

    /**
     * Override the application executor to run every task in the main thread.
     * @return
     */
    @Primary
    @Bean
    public ExecutorService taskTestExecutor() {
        return new SameThreadExecutorService();
    }
}
