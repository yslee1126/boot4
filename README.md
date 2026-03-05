# Boot4 Project

이 프로젝트는 Spring Boot 4.0.2 (Java 25) 기반의 최신 기능 테스트를 위한 샘플 애플리케이션입니다.
민감한 설정 정보는 Jasypt를 통해 암호화되어 있습니다.

## 🛠 Tech Stack

- **Java**: 25
- **Framework**: Spring Boot 4.0.2
- **Database**: SQLite (Default), MySQL/PostgreSQL (Optional via Docker)
- **ORM**: Spring Data JPA, QueryDSL 5.1.0

## 주요 테스트 기능 

- [x] **Virtual Threads (기본 활성화 강화)**  
  - [x] 동시 요청 테스트 `RoomConcurrencyTest.java` (50유저 성공)
  - [x] pinning 현상 로그 확인 (-Djdk.tracePinnedThreads=full 로 개선 확인)
  - [x] outbound HTTP에도 적용 (JDK HttpClient + Virtual Threads)

- [x] **@HttpExchange 선언형 HTTP 클라이언트**  
  - 인터페이스만으로 외부 API 호출 (RestClient보다 코드 간결)  
  - Virtual Thread + Structured Concurrency 조합으로 병렬 호출 테스트

- [ ] **API Versioning 네이티브 지원**  
  - URI / Header / Parameter 기반 버저닝 기본 제공  
  - `@ApiVersion` + `X-API-Version` 헤더로 멀티 버전 엔드포인트 테스트

- [ ] **JSpecify null-safety 포트폴리오 강화**  
  - `@NullMarked`, `@Nullable` 패키지/모듈 단위 적용  
  - 컴파일 타임 + IDE 경고 대폭 줄어드는 효과 확인

- [ ] **Structured Concurrency (preview → 안정화 방향)**  
  - `StructuredTaskScope`로 병렬 작업 구조화 + 에러/취소 명확 처리  
  - Virtual Thread와 함께 5~10개 태스크 fork/join 비교 실험

- [ ] **Scoped Values (preview)**  
  - ThreadLocal 대체로 traceId / context 전파 (MDC 대신)  
  - Virtual Thread 환경 logging + observability 체감 테스트

- [ ] **Primitive Types in Patterns (preview)**  
  - switch / instanceof에 int/long/float/double + record 패턴 매칭  
  - 복잡 DTO/primitive 파싱 로직 간소화 실험

- [ ] **Module Import Declarations (preview)**  
  - `import module java.sql;` 같은 모듈 단위 import  
  - 모듈화 프로젝트에서 import 문 정리 후 빌드/가독성 확인

- [ ] **OpenTelemetry / Observability 자동 설정 개선**  
  - `management.opentelemetry.*` 속성으로 tracing/metrics 쉽게 셋업  
  - Grafana + Tempo + Loki 연동 빠르게 띄워보기

- [ ] **Project Leyden / Native Image 최적화 (미리보기 강화)**  
  - AOT + static image로 warmup 최소화  
  - startup 시간 비교 (기존 JVM vs Native/Leyden)

## 🚀 How to Run

### 기본 실행 (SQLite)
별도의 DB 설치 없이 SQLite를 사용하여 실행할 수 있습니다.

```bash
# SQLite 사용 (기본값)
JASYPT_KEY="YOUR_SECRET_KEY_HERE" ./gradlew bootRun
```

### Docker DB 사용 (선택사항)
MySQL 또는 PostgreSQL을 사용하려면 Docker로 DB를 설치한 후 프로파일을 지정하여 실행합니다.
자세한 설치 방법은 [local-infra.md](./docs/local-infra.md)를 참고하세요.

```bash
# MySQL 사용
JASYPT_KEY="YOUR_SECRET_KEY_HERE" ./gradlew bootRun --args='--spring.profiles.active=mysql'

# PostgreSQL 사용
JASYPT_KEY="YOUR_SECRET_KEY_HERE" ./gradlew bootRun --args='--spring.profiles.active=postgres'
```

## API 스펙 
[docs/api-spec.md](./docs/api-spec.md) 파일을 참고해주세요.

---

