# Boot4 Project

이 프로젝트는 Spring Boot 4.0.2 (Java 25) 기반의 최신 기능 테스트를 위한 샘플 애플리케이션입니다.
H2 임베디드 데이터베이스와 QueryDSL을 사용하며, 민감한 설정 정보는 Jasypt를 통해 암호화되어 있습니다.

## 🛠 Tech Stack

- **Java**: 25
- **Framework**: Spring Boot 4.0.2
- **Database**: H2 (Embedded / File Systems)
- **ORM**: Spring Data JPA, QueryDSL 5.1.0
- **Security**: Jasypt (DB Password Encryption)
- **Build Tool**: Gradle

## ⚙️ Configuration

### Database (H2)
- **Mode**: File-based storage
- **Location**: `./build/h2db`
- **Console**: [thttp://localhost:8080/h2-console](http://localhost:8080/h2-console)
- **JDBC URL**: `jdbc:h2:file:./build/h2db;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE`
- **Username**: `admin`
- **Password**: `admin123` (설정 파일에는 암호화되어 저장됨)

### Jasypt Encryption
DB 비밀번호와 같은 민감한 정보는 암호화되어 `application.yml`에 설정되어 있습니다.
애플리케이션 구동 시 복호화를 위한 **Jasypt Key**가 필요합니다.

## 🚀 How to Run

보안을 위해 `JASYPT_KEY` 환경 변수를 주입하여 실행해야 합니다.

```bash
# JASYPT_KEY 값을 환경 변수로 설정하여 실행
# 실제 키 값은 보안상 공유되지 않으므로, 별도로 전달받은 키를 사용하세요.
JASYPT_KEY="YOUR_SECRET_KEY_HERE" ./gradlew bootRun
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
