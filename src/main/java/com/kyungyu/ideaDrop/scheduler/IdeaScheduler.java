package com.kyungyu.ideaDrop.scheduler;

import com.kyungyu.ideaDrop.repository.RequestRepository;
import com.kyungyu.ideaDrop.service.SlackService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IdeaScheduler {

    private final RequestRepository requestRepository;
    private final SlackService slackService;

    // 초 분 시 일 월 요일 (매일 21시 0분 0초에 실행, 한국 시간 기준)
    @Scheduled(cron = "0 0 21 * * *", zone = "Asia/Seoul")
    public void generateDailyIdea() {
        // 1. DB에서 가장 요청 조회.
        requestRepository.findTopByOrderByCreatedAtDesc().ifPresent(latestRequest -> {

            String prompt = latestRequest.getPrompt();

            // 2. 알아서 실행하고 슬랙 채널로 전송하도록 서비스 호출.
            slackService.processPromptScheduled("SYSTEM", prompt);
        });
    }
}
