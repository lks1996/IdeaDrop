package com.kyungyu.ideaDrop.config;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.kyungyu.ideaDrop.IdeaDropApplication;
import com.kyungyu.ideaDrop.scheduler.IdeaScheduler;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Map;

/**
 * AWS Lambda 환경에서의 실행 진입점.
 * EventBridge가 전달하는 JSON 페이로드를 Map<String, String> 형태로 받습니다.
 */
public class ScheduledJobHandler implements RequestHandler<Map<String, String>, String> {

    private static ConfigurableApplicationContext applicationContext;

    // 람다 '콜드 스타트(Cold Start)' 시점에 컨텍스트를 단 한 번만 초기화하여 재사용성 높임.
    static {
        // 톰캣 웹 서버가 뜨지 않고 빈(Bean)들만 아주 빠르게 메모리에 올림.
        applicationContext = SpringApplication.run(IdeaDropApplication.class);
    }

    @Override
    public String handleRequest(Map<String, String> event, Context context) {
        context.getLogger().log("Received event payload: " + event);

        String jobName = event.getOrDefault("jobName", "unknown");

        try {
            // 어떤 잡을 실행할지 결정.
            if ("daily".equalsIgnoreCase(jobName)) {
                context.getLogger().log("===== [IdeaDropper]] Starting Daily Process Job =====");
                IdeaScheduler ideaScheduler = applicationContext.getBean(IdeaScheduler.class);
                ideaScheduler.generateDailyIdea();
                return "[IdeaDropper]]Daily Job Completed Successfully";

            } else {
                context.getLogger().log("Unknown job name received: " + jobName);
                return "Error: Unknown Job Name";
            }
        } catch (Exception e) {
            context.getLogger().log("Exception occurred during job execution: " + e.getMessage());
            throw new RuntimeException("Job execution failed", e);
        }
    }
}