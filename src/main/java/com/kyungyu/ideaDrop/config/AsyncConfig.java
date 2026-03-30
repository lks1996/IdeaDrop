package com.kyungyu.ideaDrop.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {
    // 실행 환경(Windows, AWS Lambda)에 따라 비동기 설정 변경을 위함.
    // Spring profile : Windows -> 비동기
    // Spring profile : AWS Lambda -> 동기
}