# Boot4 Project

이 프로젝트는 Spring Boot 4.0.2 (Java 25) 기반의 최신 기능 테스트를 위한 샘플 애플리케이션입니다.
민감한 설정 정보는 Jasypt를 통해 암호화되어 있습니다.

## 🛠 Tech Stack

- **Java**: 25
- **Framework**: Spring Boot 4.0.2
- **Database**: SQLite (Default), MySQL/PostgreSQL (Optional via Docker)
- **ORM**: Spring Data JPA, QueryDSL 5.1.0
- **Security**: Jasypt (DB Password Encryption)
- **Build Tool**: Gradle

## 🚀 How to Run

### 기본 실행 (SQLite)
별도의 DB 설치 없이 SQLite를 사용하여 실행할 수 있습니다.

```bash
# SQLite 사용 (기본값)
JASYPT_KEY="YOUR_SECRET_KEY_HERE" ./gradlew bootRun
```

### Docker DB 사용 (선택사항)
MySQL 또는 PostgreSQL을 사용하려면 Docker로 DB를 설치한 후 프로파일을 지정하여 실행합니다.
자세한 설치 방법은 [LOCAL_INFRA.md](./LOCAL_INFRA.md)를 참고하세요.

```bash
# MySQL 사용
JASYPT_KEY="YOUR_SECRET_KEY_HERE" ./gradlew bootRun --args='--spring.profiles.active=mysql'

# PostgreSQL 사용
JASYPT_KEY="YOUR_SECRET_KEY_HERE" ./gradlew bootRun --args='--spring.profiles.active=postgres'
```

## 📝 API Usage (Examples)

### 1. Create User
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "test_user"}'
```

### 2. Get All Users
```bash
curl http://localhost:8080/api/users
```

### 3. Search User (QueryDSL)
```bash
curl "http://localhost:8080/api/users/search?name=test_user"
```

### 4. Update User
```bash
curl -X PUT http://localhost:8080/api/users/1 \
  -H "Content-Type: application/json" \
  -d '{"name": "updated_user"}'
```

### 5. Delete User
```bash
curl -X DELETE http://localhost:8080/api/users/1
```

---

## 🏗 Local Infrastructure
로컬 인프라(DB) 설치 및 구성 정보는 아래 문서를 참고하세요.
- [LOCAL_INFRA.md](./LOCAL_INFRA.md)
