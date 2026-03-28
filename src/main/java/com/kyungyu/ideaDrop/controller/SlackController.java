package com.kyungyu.ideaDrop.controller;

import com.kyungyu.ideaDrop.service.SlackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/slack")
@RequiredArgsConstructor
public class SlackController {

    private final SlackService slackService;

    @PostMapping("/events")
    public ResponseEntity<String> handleSlackEvent(@RequestBody Map<String, Object> payload) {

        String type = (String) payload.get("type");

        // 1. 슬랙의 엔드포인트 URL 검증(Challenge) 프로세스
        if ("url_verification".equals(type)) {
            return ResponseEntity.ok((String) payload.get("challenge"));
        }

        // 2. 실제 이벤트(봇 멘션 등) 수신 프로세스
        if ("event_callback".equals(type)) {
            // payload 안의 "event" 객체를 추출해.
            Map<String, Object> event = (Map<String, Object>) payload.get("event");

            // 이벤트 타입이 봇을 호출한 'app_mention'인지 확인해.
            if (event != null && "app_mention".equals(event.get("type"))) {
                String userId = (String) event.get("user");
                String text = (String) event.get("text");
                String channelId = (String) event.get("channel"); // 이벤트가 발생한 채널 ID

                // 프롬프트 텍스트에는 봇의 ID(예: <@U123456>)가 포함되어 있어.
                // 제미나이에게 불필요한 텍스트가 가지 않도록 봇 태그를 제거해 주는 것이 좋아.
                String cleanedPrompt = text.replaceAll("<@.+?>", "").trim();

                // 3. 비동기 서비스 호출
                // Slack의 3초 응답 제한을 피하기 위해, 로직은 백그라운드 스레드로 넘겨.
                slackService.processPromptEvent(userId, cleanedPrompt, channelId);
            }
        }

        // 컨트롤러는 즉시 HTTP 200 OK를 반환하여 슬랙 서버를 안심시켜.
        return ResponseEntity.ok("Event Received");
    }

    @GetMapping("/test")
    public ResponseEntity<String> testSlackMessage() {
        slackService.sendTestMessage();
        return ResponseEntity.ok("슬랙 확인해봐");
    }
}