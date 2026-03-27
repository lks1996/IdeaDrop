package com.kyungyu.ideaDrop.service;

import com.kyungyu.ideaDrop.entity.*;
import com.kyungyu.ideaDrop.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SlackService {

    private final RequestRepository requestRepository;
    private final ResponseRepository responseRepository;
    private final GeminiService geminiService;

    @Value("${slack.bot.token}")
    private String slackBotToken;

    @Value("${slack.channel.id}")
    private String slackChannelId;

    /**
     * 설정 혹은 전달 받은 프롬프트로 AI가 아이디어 제안.
     * @param userId
     * @param prompt
     * @param channelId
     */
    @Async
    public void processPromptEvent(String userId, String prompt, String channelId) {
        // 1. 요청 저장. (PENDING)
        Request request = savePendingRequest(userId, prompt);

        // 2. Gemini 아이디어 생성.
        String currentAiResult = geminiService.generateIdeas(prompt);

        // 3. 생성된 텍스트를 Gemini를 통해 Vector로 변환.
        String currentVector = geminiService.generateEmbedding(currentAiResult);

        int maxRetries = 2;         // 최대 재시도 횟수 설정 값.
        int attempt = 0;            // 현재 재시도 횟수.
        boolean isUnique = false;   // 중복값 검증 결과값.
        Optional<Response> similarResponse = Optional.empty();

        // 4. 코사인 거리가 0.15 미만인 가장 비슷한 기존 아이디어 조회.
        while (attempt < maxRetries) {
            double similarityThreshold = 0.15;
            similarResponse = responseRepository.findMostSimilarIdea(currentVector, similarityThreshold);

            if (similarResponse.isEmpty()) {
                isUnique = true; // 중복이 없으므로 루프 탈출.
                break;
            }

            // 중복이 발견되면 재시도 카운트 증가 및 새로운 프롬프트로 재호출.
            attempt++;
            String pastIdea = similarResponse.get().getOutput();
            currentAiResult = geminiService.generateIdeasAvoidingPast(prompt, pastIdea);
            currentVector = geminiService.generateEmbedding(currentAiResult);
        }

        // 5. 루프 종료 후 최종 메시지 구성.
        String finalSlackMessage = currentAiResult;

        if (!isUnique && similarResponse.isPresent()) {
            // maxRetries 시도했으나 비슷하게 나왔을 경우의 방어(Fallback) 로직.
            String pastIdea = similarResponse.get().getOutput();
            finalSlackMessage += "\n\n⚠️ *[알림] 시스템이 " + maxRetries + "회 재시도했으나, 여전히 과거와 유사한 아이디어가 도출됨:*\n" + pastIdea;
        }

        // 6. Response 저장. (텍스트와 벡터 함께 저장)
        saveResponse(request, currentAiResult, currentVector);

        // 7. 상태 업데이트 (SUCCESS) 및 Slack 메시지 전송.
        request.markSuccess();
        requestRepository.save(request);
        sendSlackMessageWithToken(channelId, finalSlackMessage);
    }

    /**
     * 최종 결과 메시지를 slack 형식에 맞게 파싱 후 전송.
     * @param channelId
     * @param text
     */
    private void sendSlackMessageWithToken(String channelId, String text) {
        RestTemplate restTemplate = new RestTemplate();
        String slackApiUrl = "https://slack.com/api/chat.postMessage";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Authorization 헤더에 봇 토큰 추가
        headers.set("Authorization", "Bearer " + slackBotToken);

        Map<String, String> body = Map.of(
                "channel", channelId,
                "text", text
        );

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForEntity(slackApiUrl, request, String.class);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("슬랙 자동 메시지 전송 실패");
        }
    }

    /**
     * 사용자로부터 전달 받은 요청 PENDING 상태로 DB에 저장.
     * @param userId
     * @param prompt
     * @return
     */
    private Request savePendingRequest(String userId, String prompt) {
        Request request = new Request();
        request.setSlackUserId(userId);
        request.setPrompt(prompt);
        request.setStatus(Status.PENDING);
        request.setCreatedAt(LocalDateTime.now());

        return requestRepository.save(request);
    }

    /**
     * 사용자에게 전달할 응답을 DB에 저장.
     * @param request
     * @param aiResult
     * @param vectorData
     * @return
     */
    private Response saveResponse(Request request, String aiResult, String vectorData) {
        Response response = new Response();
        response.setRequest(request);
        response.setOutput(aiResult);
        response.setEmbeddingVector(vectorData);
        response.setLikeCount(0);
        response.setCreatedAt(LocalDateTime.now());
        return responseRepository.save(response);
    }

    public void sendTestMessage() {
        String testMessage = "🚀 *[IdeaDrop 테스트]*\n스프링 부트 서버에서 Bot Token을 사용하여 성공적으로 메시지를 전송했습니다!";

        // application.yml에서 주입받은 slackChannelId를 타겟으로 전송
        sendSlackMessageWithToken(slackChannelId, testMessage);
    }
}