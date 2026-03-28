package com.kyungyu.ideaDrop.controller;

import com.kyungyu.ideaDrop.entity.Response;
import com.kyungyu.ideaDrop.service.SlackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@RestController
@RequestMapping("/slack")
@RequiredArgsConstructor
public class SlackController {

    private final SlackService slackService;
    private final ObjectMapper objectMapper;

    @PostMapping("/events")
    public ResponseEntity<String> handleSlackEvent(@RequestBody Map<String, Object> payload) throws Exception {

        try {
            String type = (String) payload.get("type");

            // 1. 슬랙의 엔드포인트 URL 검증(Challenge) 프로세스
            if ("url_verification".equals(type)) {
                return ResponseEntity.ok((String) payload.get("challenge"));
            }

            // 2. 실제 이벤트(봇 멘션 등) 수신 프로세스
            if ("event_callback".equals(type)) {

                Map<String, Object> event = (Map<String, Object>) payload.get("event");

                if (event != null && "app_mention".equals(event.get("type"))) {
                    String userId = (String) event.get("user");
                    String text = (String) event.get("text");
                    String channelId = (String) event.get("channel"); // 이벤트가 발생한 채널 ID

                    String cleanedPrompt = text.replaceAll("<@.+?>", "").trim();

                    // 3. 비동기 서비스 호출
                    slackService.processPromptEvent(userId, cleanedPrompt, channelId);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        // 컨트롤러는 즉시 HTTP 200 OK를 반환.
        return ResponseEntity.ok("Event Received");
    }

    @PostMapping(value = "/action", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> handleSlackAction(@RequestParam("payload") String payload) throws Exception {
        try {
            // 1. 눌린 버튼의 action_id와 숨겨둔 value(DB ID)를 추출
            JsonNode rootNode = objectMapper.readTree(payload);
            String actionId = rootNode.path("action_id").asText();
            Long responseId = rootNode.path("value").asLong();

            if ("like_idea_action".equals(actionId)) {
                String updatedMessageJson = slackService.likeIdeaActionEvent(responseId, payload);

                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(updatedMessageJson);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.ok().build();
    }


    @GetMapping("/test")
    public ResponseEntity<String> testSlackMessage() {
        slackService.sendTestMessage();
        return ResponseEntity.ok("슬랙 확인해봐");
    }
}