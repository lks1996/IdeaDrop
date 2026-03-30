package com.kyungyu.ideaDrop.scheduler;

import com.kyungyu.ideaDrop.entity.Request;
import com.kyungyu.ideaDrop.entity.Status;
import com.kyungyu.ideaDrop.repository.RequestRepository;
import com.kyungyu.ideaDrop.service.SlackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class IdeaScheduler {

    private final RequestRepository requestRepository;
    private final SlackService slackService;

    @Value("${slack.channel.id}")
    private String slackChannelId;

    // 초 분 시 일 월 요일 (매일 18시 0분 0초에 실행, 한국 시간 기준)
//    @Scheduled(cron = "0 20 18 * * *", zone = "Asia/Seoul")
    public void generateDailyIdea() {
        // 1. 디비에서 사용자의 성공한 프롬프트 조회.
        Optional<Request> lastRequest = requestRepository.findFirstByStatusOrderByCreatedAtDesc(Status.SUCCESS);

        if (lastRequest.isEmpty()) {
            log.error("디비에 저장된 이전 프롬프트가 없어서 자동 생성 스킵.");
            return;
        }

        // 2. 가장 최근 프롬프트를 추출.
        String savedPrompt = lastRequest.get().getPrompt();
        String systemUserId = "SYSTEM_SCHEDULER";

        log.info("스케줄러 작동: 최근 주제 '{}' 기반으로 아이디어 자동 생성 시작.", savedPrompt);

        slackService.processPromptEvent(systemUserId, savedPrompt, slackChannelId);
    }
}
