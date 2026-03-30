# 1단계: 프로젝트를 빌드하기 위한 빌더(Builder) 환경
# Eclipse Temurin의 JDK 17 이미지 기반
FROM eclipse-temurin:21-jdk-jammy as builder

# 작업 디렉토리 설정
WORKDIR /workspace/app

# Gradle Wrapper 파일들을 먼저 복사
COPY gradlew .
COPY gradle gradle

# build.gradle 파일을 복사
COPY build.gradle .

# (만약 settings.gradle 파일이 있다면)
# COPY settings.gradle .

# 의존성을 먼저 다운로드하여 캐싱 효과를 극대화
RUN ./gradlew dependencies

# 나머지 소스 코드를 복사
COPY src src

# Gradle을 사용하여 프로젝트를 빌드 (실행 가능한 JAR 생성)
RUN ./gradlew bootJar

# 2단계: 실제 애플리케이션을 실행하기 위한 최종 환경
# 더 가볍고 안전한 JRE(Java Runtime Environment) 이미지를 사용
FROM eclipse-temurin:21-jre-jammy

# 타임존 설정 (한국시간)
RUN apt-get update && apt-get install -y tzdata unzip \
    && ln -sf /usr/share/zoneinfo/Asia/Seoul /etc/localtime \
    && echo "Asia/Seoul" > /etc/timezone \
    && apt-get clean

# 작업 디렉토리 설정
WORKDIR /app

# 빌더 환경에서 만들어진 JAR 파일을 최종 환경으로 복사
COPY --from=builder /workspace/app/build/libs/*.jar app.jar

RUN unzip app.jar -d unpacked

# 빌드 시점에 어떤 모드로 구동할지 정하는 인자
ARG RUN_MODE=windows
ENV APP_RUN_MODE=${RUN_MODE}

# 컨테이너 내부에 /config 라는 빈 디렉토리 셍성. (application.yml 파일용)
VOLUME /config

# 쉘 스크립트를 사용하여 환경변수에 따라 실행 명령어를 동적으로 변경.
# (상단 생략)
ENTRYPOINT ["sh", "-c", "\
  if [ \"$APP_RUN_MODE\" = \"lambda\" ]; then \
    echo '===== Starting AWS Lambda RIC (Event Listener Mode) ====='; \
    exec java -cp /app/unpacked/BOOT-INF/classes:/app/unpacked/BOOT-INF/lib/*:/app/unpacked/META-INF com.amazonaws.services.lambda.runtime.api.client.AWSLambda com.kyungyu.ideaDrop.config.ScheduledJobHandler; \
  else \
    echo '===== Starting Spring Boot Application (Windows Web Mode) ====='; \
    exec java -jar /app/app.jar; \
  fi \
"]