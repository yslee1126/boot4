# Local Infrastructure Setup

로컬 개발 환경 구축을 위한 가이드입니다.

## 🐬 MySQL Database (Docker)

최신 버전의 MySQL을 리소스를 최소화하여 설치하는 방법입니다.

### 1. MySQL 설치 및 실행
아래 명령어를 통해 Docker 컨테이너를 실행합니다. 메모리 사용을 줄이기 위해 성능 모니터링 옵션을 끄고 버퍼 크기를 조정했습니다.

```bash
docker run -d \
  --name mysql-local \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=YOUR_ROOT_PASSWORD \
  --security-opt seccomp=unconfined \
  --memory=1g \
  --restart always \
  mysql:latest \
  --performance_schema=OFF \
  --innodb_buffer_pool_size=64M \
  --innodb_log_buffer_size=1M \
  --max_connections=10
```

**⚠️ 주의**: `YOUR_ROOT_PASSWORD`를 실제 사용할 비밀번호로 변경하세요.

### 2. 주요 설정 상세
- **Port**: `3306`
- **Root Password**: 설정한 비밀번호
- **Optimization**:
  - `performance_schema=OFF`: 메모리 점유율을 대폭 낮춤
  - `memory=1g`: 컨테이너 메모리 제한
  - `innodb_buffer_pool_size=64M`: 버퍼 풀 크기 축소

## 🐘 PostgreSQL Database (Docker)

최소한의 리소스로 PostgreSQL을 실행하는 방법입니다.

### 1. PostgreSQL 설치 및 실행
```bash
docker run -d \
  --name postgres-local \
  -p 5432:5432 \
  -e POSTGRES_PASSWORD=YOUR_PASSWORD \
  -e POSTGRES_DB=boot4_db \
  --memory=512m \
  --restart always \
  postgres:latest \
  -c 'shared_buffers=16MB' \
  -c 'max_connections=10'
```

**⚠️ 주의**: `YOUR_PASSWORD`를 실제 사용할 비밀번호로 변경하세요.

### 2. 주요 설정 상세
- **Port**: `5432`
- **Password**: 설정한 비밀번호
- **Optimization**:
  - `memory=512m`: 컨테이너 메모리 제한
  - `shared_buffers=16MB`: 공유 버퍼 크기 최소화
  - `max_connections=10`: 동시 접속자 수 제한

## 📊 LGTM Stack (Observability) 올인원 실행

OpenTelemetry를 활용한 통합 모니터링 및 추적(Observability) 환경을 손쉽게 구성하기 위해 Grafana의 LGTM 스택 올인원 이미지를 사용할 수 있습니다.

### LGTM 스택 구성 요소
LGTM은 다음 4가지 핵심 모니터링/추적 시스템의 앞글자를 딴 것입니다:
- **L**oki: 분산 로그 수집 및 관리 (Logs)
- **G**rafana: 수집된 데이터(로그, 메트릭, 트레이스)를 시각화하는 대시보드 (Dashboards)
- **T**empo: 분산 추적 시스템 (Traces)
- **M**imir: 확장 가능한 메트릭 저장소 (Metrics, Prometheus 기반)

해당 올인원(`grafana/otel-lgtm`) 이미지에는 위 4가지 구성 요소와 데이터를 수집 및 라우팅하는 **OpenTelemetry Collector**가 모두 포함되어 한 번에 실행 가능합니다.

### 1. LGTM 올인원 설치 및 실행
```bash
docker run -d \
  --name lgtm-local \
  -p 3000:3000 \
  -p 4317:4317 \
  -p 4318:4318 \
  --rm \
  grafana/otel-lgtm:latest
```

### 2. 주요 설정 상세
- **포트 정보**:
  - `3000`: Grafana 대시보드 웹 UI (초기 계정: `admin` / `admin`)
  - `4317`: OTLP gRPC 포트 (Spring Boot에서 주로 데이터를 쏘는 기본 엔드포인트)
  - `4318`: OTLP HTTP 포트

---

## 🚀 Application Execution

애플리케이션을 실행할 때는 해당 데이터베이스 프로파일을 활성화해야 합니다.

```bash
# MySQL 사용 시
JASYPT_KEY="YOUR_JASYPT_KEY" ./gradlew bootRun --args='--spring.profiles.active=mysql'

# PostgreSQL 사용 시
JASYPT_KEY="YOUR_JASYPT_KEY" ./gradlew bootRun --args='--spring.profiles.active=postgres'
```
