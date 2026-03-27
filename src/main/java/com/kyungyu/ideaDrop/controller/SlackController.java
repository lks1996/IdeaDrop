package com.kyungyu.ideaDrop.controller;

import com.kyungyu.ideaDrop.service.SlackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/slack")
@RequiredArgsConstructor
public class SlackController {

    private final SlackService slackService;

    @PostMapping("/command")
    public ResponseEntity<String> handleCommand(
            @RequestParam("text") String text,
            @RequestParam("user_id") String userId,
            @RequestParam("response_url") String responseUrl
    ) {
        // 비동기 메서드 호출
        slackService.processPrompt(userId, text, responseUrl);

        return ResponseEntity.ok("아이디어를 생성 중.. ㄱㄷㄱㄷ");
    }
}