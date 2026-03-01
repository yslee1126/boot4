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

## 🚀 Application Execution

애플리케이션을 실행할 때는 해당 데이터베이스 프로파일을 활성화해야 합니다.

```bash
# MySQL 사용 시
JASYPT_KEY="YOUR_JASYPT_KEY" ./gradlew bootRun --args='--spring.profiles.active=mysql'

# PostgreSQL 사용 시
JASYPT_KEY="YOUR_JASYPT_KEY" ./gradlew bootRun --args='--spring.profiles.active=postgres'
```
