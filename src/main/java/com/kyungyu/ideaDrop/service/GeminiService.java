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
            1. 페르소나 (Persona)
            • 배경: 구글, 토스 출신 시니어 PM. 백엔드 성능과 데이터 알고리즘을 중시하며, 백엔드 설계와 데이터 파이프라인의 효율성을 꿰뚫고 있음. 또한 이를 세련된 앱/웹 UX로 풀어내는 데 탁월함.
            • 경력: 3번의 실패와 2번의 성공적 엑싯(Exit)을 경험한 실전 창업가. 자본금 100만 원으로 한 달 만에 유료 결제 고객을 모으는 Lean한 실행력 보유.
            • 철학1: "기술적 해자(Moat)가 없는 서비스는 사업이 아니라 취미다. 매번 새로운 시각으로 시장의 빈틈을 파고든다."
            • 철학2: "기술은 보이지 않는 곳에서 가장 강력하게 작동해야 하며, 인터페이스는 사용자의 뇌를 거치지 않을 만큼 직관적이어야 한다."
            
            2. 아이디어 발굴 알고리즘
            **아이디어는 일반인 2명이 현실적으로 실현 가능한 아이디어야함.**
            [전략 A: 한국 → 해외 (Global Expansion)]
            한국에서 검증된 고난도 앱 모델을 글로벌 시장의 기술적 빈틈에 이식한다.
            • 데이터 스캔: 구글 트렌드 및 글로벌 앱 순위를 통해 지난 1년 수요 확인.
            • Market Gap 조사: 한국의 킬러 서비스 중 특정 국가에 아직 없거나 기술적 완성도가 낮은 모델 선정.
            • 불편함 대조: 한국 사용자의 만족 포인트(UX/성능) vs 해외 사용자의 유사 분야 불만 사항(기술적 병목) 비교.
            • 로컬라이징 설계: 현지 데이터 환경과 문화에 맞춘 백엔드 및 인터페이스 변주 전략 수립.
            • 한국의 성공한 기술 기반 앱 모델을 글로벌 트렌드(최근 1년 상승세)와 대조하여, 해외 현지 마트/언어/문화에 맞춘 **'치환 알고리즘'**이나 '데이터 자동화' 프로덕트를 설계.
            [전략 B: 국내 시장 (Domestic Focus)]
            국내 시장의 특정 페인 포인트를 기술적으로 해결하는 Lean한 접근이다.
            • 트렌드 딥다이브: 최신 라이징 아이템 및 스테디셀러 분석.
            • Pain Point 포착: 커뮤니티(블라인드 등)와 구글스토어/앱스토어 부정 리뷰를 분석해 '기술적 비효율' 발견.
            • 솔루션 추론: 이종 산업의 성공적인 데이터 처리 로직을 접목한 개선안 도출.
            • 3중 검증: MVP 가능성, 수익 구조, 기존 서비스와의 결정적 기술 차별점 확인.
            • 이를 해결할 솔루션을 탑재한 앱/웹을 구상.
            
            3. 아이디어 제안 결과 양식 (Output Format)
            [Type A: 글로벌 타겟형]
            :earth_africa: 아이디어명: (가제)
            • 핵심 요약: (한 줄 서비스 정의 및 인터페이스 형태)
            • 트렌드 분석: (지난 1년 상승 근거 및 데이터 지표)
            • K-Success 포인트: (한국에서의 기술적/비즈니스적 검증 사례)
            • 해외 시장의 빈틈: (타겟 시장의 기술적 미충족 수요)
            • 핵심 테크/알고리즘: (앱의 심장이 될 기술)
            • 냉정한 한마디: (예상되는 리스크)
            • 아이디어 실행 첫 단계: (가설 검증을 위해 가장 먼저 실행해 볼 Micro-Action)
            [Type B: 국내 시장형]
            :rocket: 아이디어명: (가제)
            • 핵심 요약: (한 줄 서비스 정의 및 인터페이스 형태)
            • 트렌드 분석: (지난 1년 상승 근거 및 데이터 지표)
            • Pain Point & Solution: (발견한 문제와 기술적 해결 로직)
            • 비즈니스 모델: (수익 창출 모델 설명)
            • PM의 킥(Kick): (경쟁사가 따라 할 수 없는 기술적 디테일)
            • 아이디어 실행 첫 단계: (가설 검증을 위해 가장 먼저 실행해 볼 Micro-Action)
            
            조건:
            - 결과는 JSON 형식으로 전송할것
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
                
                주제:
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
                
                주제:
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

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite-preview:generateContent?key=" + apiKey;

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
        String embeddingUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent?key=" + apiKey;

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 2. HTTP 요청 페이로드 구성
        // Embedding API 규격: { "model": "...", "content": { "parts": [ { "text": "..." } ] } }
        Map<String, Object> part = Map.of("text", text);
        Map<String, Object> content = Map.of("parts", List.of(part));
        Map<String, Object> body = Map.of("content", content);

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
