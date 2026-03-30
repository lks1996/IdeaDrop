package com.kyungyu.ideaDrop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.concurrent.Executor;

@Configuration
@Profile("lambda")
public class SyncTaskExecutorConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        return Runnable::run; // 👈 동기 실행
    }
}