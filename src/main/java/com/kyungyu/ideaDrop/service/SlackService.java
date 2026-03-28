package com.kyungyu.ideaDrop.service;

import com.kyungyu.ideaDrop.dto.GeminiIdeaResponse;
import com.kyungyu.ideaDrop.entity.*;
import com.kyungyu.ideaDrop.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlackService {

    private final RequestRepository requestRepository;
    private final ResponseRepository responseRepository;
    private final GeminiService geminiService;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
        try {
            // 2. Gemini 아이디어 생성.
            String currentAiResult = geminiService.generateIdeas(prompt);

            // 3. JSON 파싱 및 신호(Signal) 추출
            GeminiIdeaResponse responseDto = objectMapper.readValue(currentAiResult, GeminiIdeaResponse.class);

            String pureSignal = responseDto.typeAGlobal().ideaName() + " " + responseDto.typeAGlobal().coreSummary() + " "
                    + responseDto.typeBDomestic().ideaName() + " " + responseDto.typeBDomestic().coreSummary();

            // 4. 생성된 텍스트를 Gemini를 통해 Vector로 변환.
            String currentVector = geminiService.generateEmbedding(pureSignal);

            int maxRetries = 2;         // 최대 재시도 횟수 설정 값.
            int attempt = 0;            // 현재 재시도 횟수.
            boolean isUnique = false;   // 중복값 검증 결과값.
            Optional<Response> similarResponse = Optional.empty();

            // 5. 코사인 거리가 0.15 미만인 가장 비슷한 기존 아이디어 조회.
            while (attempt < maxRetries) {
                double similarityThreshold = 0.15;
                similarResponse = findMostSimilarIdeaLocally(currentVector, similarityThreshold);

                if (similarResponse.isEmpty()) {
                    isUnique = true; // 중복이 없으므로 루프 탈출.
                    break;
                }

                // 중복이 발견되면 재시도 카운트 증가 및 새로운 프롬프트로 재호출.
                attempt++;
                String pastIdea = similarResponse.get().getOutput();
                currentAiResult = geminiService.generateIdeasAvoidingPast(prompt, pastIdea);
                responseDto = objectMapper.readValue(currentAiResult, GeminiIdeaResponse.class);
                pureSignal = responseDto.typeAGlobal().ideaName() + " " + responseDto.typeAGlobal().coreSummary() + " "
                        + responseDto.typeBDomestic().ideaName() + " " + responseDto.typeBDomestic().coreSummary();
                currentVector = geminiService.generateEmbedding(pureSignal);
            }

            // 6. 루프 종료 후 최종 메시지 구성.
            String finalSlackMessage = formatToSlackMarkdown(responseDto);

            if (!isUnique && similarResponse.isPresent()) {
                // maxRetries 시도했으나 비슷하게 나왔을 경우의 방어(Fallback) 로직.
                String pastIdea = similarResponse.get().getOutput();
                finalSlackMessage += "\n\n⚠️ *[알림] 시스템이 " + maxRetries + "회 재시도했으나, 여전히 과거와 유사한 아이디어가 도출됨:*\n" + pastIdea;
            }

            // 7. Response 저장. (텍스트와 벡터 함께 저장)
            Response savedResponse = saveResponse(request, currentAiResult, currentVector);

            // 8. 상태 업데이트 (SUCCESS)
            request.markSuccess();
            requestRepository.save(request);

            // 9. Block Kit 메서드로 슬랙에 전송
            sendSlackBlockMessage(channelId, finalSlackMessage, savedResponse.getId());

        }catch (Exception e){
            log.error("프로세스 처리 중 에러 발생: " , e.getMessage());

            // 1) 요청 상태를 ERROR로 업데이트
            request.setStatus(Status.ERROR);
            requestRepository.save(request);

            // 2) 사용자에게 슬랙으로 에러 메시지 전송
            String errorMessage = "⚠️ *[오류]* 아이디어를 생성 혹은 검증하는 과정에서 문제 발생. 잠시 후 다시 호출 바람.";
            sendSlackMessageWithToken(channelId, errorMessage);
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

    /**
     * JSON 파싱 메서드.
     * @param mapper
     * @param jsonResult
     * @return
     * @throws Exception
     */
    private String extractSignalFromJson(ObjectMapper mapper, String jsonResult) throws Exception {
        JsonNode rootNode = mapper.readTree(jsonResult);

        String titleA = rootNode.path("typeAGlobal").path("ideaName").asText("");
        String summaryA = rootNode.path("typeAGlobal").path("coreSummary").asText("");

        String titleB = rootNode.path("typeBDomestic").path("ideaName").asText("");
        String summaryB = rootNode.path("typeBDomestic").path("coreSummary").asText("");

        // 의미 있는 텍스트만 하나의 문장으로 결합
        return titleA + " " + summaryA + " " + titleB + " " + summaryB;
    }

    /**
     * 메모리 상에서 가장 비슷한 아이디어를 찾는 메서드
     * @param currentVectorStr
     * @param distanceThreshold
     * @return
     */
    private Optional<Response> findMostSimilarIdeaLocally(String currentVectorStr, double distanceThreshold) {
        List<Response> recentResponses = responseRepository.findTop15ByOrderByCreatedAtDesc();
        List<Double> currentVector = parseVectorString(currentVectorStr);

        Response mostSimilar = null;
        double minDistance = Double.MAX_VALUE;

        // DB에서 가져온 최근 아이디어들과 하나씩 거리를 비교해
        for (Response response : recentResponses) {
            if (response.getEmbeddingVector() == null) continue;

            List<Double> targetVector = parseVectorString(response.getEmbeddingVector());

            // 코사인 거리 = 1 - 코사인 유사도
            double distance = 1.0 - calculateCosineSimilarity(currentVector, targetVector);
            System.out.println("비교 대상 ID: " + response.getId() + " | 계산된 거리: " + distance);

            if (distance < minDistance) {
                minDistance = distance;
                mostSimilar = response;
            }
        }

        // 가장 가까운 거리가 기준치(0.15)보다 작으면 중복으로 판정
        if (minDistance < distanceThreshold) {
            return Optional.of(mostSimilar);
        }
        return Optional.empty();
    }

    /**
     * 문자열 "[0.1, -0.2, ...]" 형태를 자바 List<Double> 객체로 파싱
     * @param vectorStr
     * @return
     */
    private List<Double> parseVectorString(String vectorStr) {
        String cleanString = vectorStr.replace("[", "").replace("]", "");
        return java.util.Arrays.stream(cleanString.split(","))
                .map(String::trim)
                .map(Double::parseDouble)
                .toList();
    }

    /**
     * 코사인 유사도 수학 공식을 자바로 구현
     * @param vectorA
     * @param vectorB
     * @return
     */
    private double calculateCosineSimilarity(List<Double> vectorA, List<Double> vectorB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.size(); i++) {
            dotProduct += vectorA.get(i) * vectorB.get(i);
            normA += Math.pow(vectorA.get(i), 2);
            normB += Math.pow(vectorB.get(i), 2);
        }

        if (normA == 0 || normB == 0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 슬랙 마크다운 형식의 문자열로 포맷팅.
     * @param dto
     * @return
     */
    private String formatToSlackMarkdown(GeminiIdeaResponse dto) {
        StringBuilder sb = new StringBuilder();

        if (dto.typeAGlobal() != null) {
            GeminiIdeaResponse.TypeAGlobal global = dto.typeAGlobal();
            sb.append("🌍 *[Type A: 글로벌 타겟형]*\n");
            sb.append(">• *아이디어명:* ").append(global.ideaName()).append("\n");
            sb.append(">• *핵심 요약:* ").append(global.coreSummary()).append("\n");
            sb.append(">• *트렌드 분석:* ").append(global.trendAnalysis()).append("\n");
            sb.append(">• *K-Success 포인트:* ").append(global.kSuccessPoint()).append("\n");
            sb.append(">• *해외 시장 빈틈:* ").append(global.globalMarketGap()).append("\n");
            sb.append(">• *핵심 테크/알고리즘:* ").append(global.coreTech()).append("\n");
            sb.append(">• *냉정한 한마디:* ").append(global.riskAssessment()).append("\n");
            sb.append(">• *실행 첫 단계:* ").append(global.firstStep()).append("\n\n");
        }

        if (dto.typeBDomestic() != null) {
            GeminiIdeaResponse.TypeBDomestic domestic = dto.typeBDomestic();
            sb.append("🇰🇷 *[Type B: 국내 시장형]*\n");
            sb.append(">• *아이디어명:* ").append(domestic.ideaName()).append("\n");
            sb.append(">• *핵심 요약:* ").append(domestic.coreSummary()).append("\n");
            sb.append(">• *트렌드 분석:* ").append(domestic.trendAnalysis()).append("\n");
            sb.append(">• *Pain Point & Solution:* ").append(domestic.painPointSolution()).append("\n");
            sb.append(">• *비즈니스 모델:* ").append(domestic.businessModel()).append("\n");
            sb.append(">• *PM의 킥:* ").append(domestic.pmKick()).append("\n");
            sb.append(">• *실행 첫 단계:* ").append(domestic.firstStep()).append("\n");
        }

        return sb.toString();
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
     * 최종 결과 메시지를 slack 버튼(Block Kit)을 포함해서 파싱 후 전송.
     * @param channelId
     * @param markdownText
     * @param responseId
     */
    private void sendSlackBlockMessage(String channelId, String markdownText, Long responseId) {
        RestTemplate restTemplate = new RestTemplate();
        String slackApiUrl = "https://slack.com/api/chat.postMessage";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + slackBotToken);

        Map<String, Object> body = Map.of(
                "channel", channelId,
                "text", "새로운 아이디어 제안이 도착했습니다.", // 푸시 알림창에 뜨는 요약 텍스트
                "blocks", List.of(
                        // 첫 번째 블록: 마크다운 텍스트
                        Map.of(
                                "type", "section",
                                "text", Map.of(
                                        "type", "mrkdwn",
                                        "text", markdownText
                                )
                        ),
                        // 두 번째 블록: 좋아요 버튼
                        Map.of(
                                "type", "actions",
                                "elements", List.of(
                                        Map.of(
                                                "type", "button",
                                                "text", Map.of(
                                                        "type", "plain_text",
                                                        "emoji", true,
                                                        "text", "👍 좋아요 (0)"
                                                ),
                                                "value", String.valueOf(responseId),
                                                "action_id", "like_idea_action"
                                        )
                                )
                        )
                )
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForEntity(slackApiUrl, request, String.class);
        } catch (Exception e) {
            log.error("슬랙 Block Kit 메시지 전송 실패: ", e);
        }
    }

    public String likeIdeaActionEvent(Long responseId, String payload) throws Exception {

        JsonNode rootNode = objectMapper.readTree(payload);

        // 2. DB에서 아이디어를 찾고 좋아요 카운트 증가
        Response ideaResponse = responseRepository.findById(responseId).orElseThrow();
        ideaResponse.setLikeCount(ideaResponse.getLikeCount() + 1);
        responseRepository.save(ideaResponse);

        // 3. 기존 메시지의 텍스트를 그대로 가져오고 버튼 숫자만 바꿔치기
        // (실제로는 기존 블록 구조를 복사해서 카운트만 수정한 JSON을 리턴해야 해)
        String originalText = rootNode.path("message").path("blocks").get(0).path("text").path("text").asText();
        int newLikeCount = ideaResponse.getLikeCount();

        return """
                {
                  "replace_original": true,
                  "blocks": [
                    {
                      "type": "section",
                      "text": {
                        "type": "mrkdwn",
                        "text": %s
                      }
                    },
                    {
                      "type": "actions",
                      "elements": [
                        {
                          "type": "button",
                          "text": {
                            "type": "plain_text",
                            "emoji": true,
                            "text": "👍 좋아요 (%d)"
                          },
                          "value": "%d",
                          "action_id": "like_idea_action"
                        }
                      ]
                    }
                  ]
                }
                """.formatted(
                objectMapper.writeValueAsString(originalText),
                newLikeCount,
                responseId
        );
    }
}