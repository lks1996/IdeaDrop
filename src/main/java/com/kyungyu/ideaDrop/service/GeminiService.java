package com.kyungyu.ideaDrop.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final String basePrompt =
            """ 
            사용자의 요청을 기반으로 창업 아이디어 1개를 생성하라.
            조건:
            - 반드시 1개
            - 결과는 JSON 형식으로 전송할것
            - 각 항목은 "제목 - 설명" 형식이며, 제목은 title, 설명은 detail 이라는 key에 담아서 전달
            - JSON형식의 결과 외에 불필요한 말 금지
            """;


    /**
     * 지정된 프롬프트로 아이디어 호출을 위한 프롬프트 생성 및 제미나이 호출.
     * @param userInput
     * @return
     */
    public String generateIdeas(String userInput) {
        String prompt = """
                %s
                
                사용자 요청:
                %s
                """.formatted(basePrompt, userInput);

        return callGeminiApi(prompt);
    }


    /**
     * 과거 아이디어를 피해서 아이디어를 재생성하도록 하는 프롬프트를 생성 후 제미나이 호출.
     * @param userInput
     * @param pastIdea
     * @return
     */
    public String generateIdeasAvoidingPast(String userInput, String pastIdea) {
        String prompt = """
                %s
                
                사용자 요청:
                %s
                
                단, 아래에 제시된 [과거에 제안된 아이디어]와는 전혀 다른 새로운 방향의 아이디어여야 한다.
                과거에 제안된 아이디어 (이것과 겹치지 않게 생성할 것):
                %s
                """.formatted(basePrompt, userInput, pastIdea);

        return callGeminiApi(prompt);
    }


    /**
     * 제미나이 API 호출부.
     * @param prompt
     * @return
     */
    private String callGeminiApi(String prompt) {

        String url = "https://generativelanguage.googleapis.com/v1/models/gemini-3.1-flash-lite-preview:generateContent?key=" + apiKey;

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> part = Map.of("text", prompt);
        Map<String, Object> content = Map.of("parts", List.of(part));
        Map<String, Object> body = Map.of("contents", List.of(content));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            return extractText(response.getBody());
        } catch (Exception e) {
            e.printStackTrace();
            return "아이디어 생성 중 오류 발생.";
        }
    }

    /**
     * 제미나이 응답 추출.
     * @param body
     * @return
     */
    private String extractText(Map body) {
        try {
            List candidates = (List) body.get("candidates");
            Map first = (Map) candidates.get(0);
            Map content = (Map) first.get("content");
            List parts = (List) content.get("parts");
            Map textPart = (Map) parts.get(0);

            return (String) textPart.get("text");
        } catch (Exception e) {
            return "AI 응답 파싱 실패";
        }
    }

    /**
     * 제미나이가 제시한 아이디어를 백터로 변환하도록 제미나이 임베딩 API 호출.
     * @param text
     * @return
     */
    public String generateEmbedding(String text) {
        // 1. Gemini Embedding 전용 API URL 세팅
        String embeddingUrl = "https://generativelanguage.googleapis.com/v1/models/gemini-3.1-flash-lite-preview:generateContent?key=" + apiKey;

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 2. HTTP 요청 페이로드 구성
        // Embedding API 규격: { "model": "...", "content": { "parts": [ { "text": "..." } ] } }
        Map<String, Object> part = Map.of("text", text);
        Map<String, Object> content = Map.of("parts", List.of(part));
        Map<String, Object> body = Map.of(
                "model", "models/text-embedding-004",
                "content", content
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            // 3. API 호출
            ResponseEntity<Map> response = restTemplate.postForEntity(embeddingUrl, request, Map.class);

            // 4. 반환된 JSON에서 벡터 배열 추출
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || !responseBody.containsKey("embedding")) {
                throw new RuntimeException("API 응답에 임베딩 데이터가 없습니다.");
            }

            Map<String, Object> embedding = (Map<String, Object>) responseBody.get("embedding");
            List<Double> values = (List<Double>) embedding.get("values");

            // 5. DB 저장을 위해 직렬화
            // List<Double>의 toString()은 "[0.012, -0.045, 0.123]" 형태의 문자열을 반환.
            return values.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "[]";
        }
    }
}
