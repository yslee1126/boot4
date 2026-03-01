# Chatbot API Specification

> Base URL: `http://localhost:8080`  
> Content-Type: `application/json`

---

## 공통 응답 형식

### 에러 응답
서비스 레이어에서 `IllegalArgumentException` 발생 시 Spring 기본 오류 응답이 반환됩니다.

---

## 1. Room API

채팅방을 생성하고 관리하는 API입니다.

### 1-1. 채팅방 생성

```
POST /api/rooms
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| title | String | ✅ | 채팅방 제목 |

**Request Example**
```json
{
  "title": "나의 챗봇 대화방"
}
```

**Response** `201 Created`

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | 채팅방 ID (자동 증가) |
| title | String | 채팅방 제목 |
| createdAt | String | 생성 시각 (ISO 8601) |

**Response Example**
```json
{
  "id": 1,
  "title": "나의 챗봇 대화방",
  "createdAt": "2026-03-01T10:00:00"
}
```

---

### 1-2. 전체 채팅방 조회

```
GET /api/rooms
```

**Response** `200 OK`

```json
[
  {
    "id": 1,
    "title": "나의 챗봇 대화방",
    "createdAt": "2026-03-01T10:00:00"
  },
  {
    "id": 2,
    "title": "두 번째 방",
    "createdAt": "2026-03-01T11:00:00"
  }
]
```

---

### 1-3. 채팅방 단건 조회

```
GET /api/rooms/{roomId}
```

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| roomId | Long | 채팅방 ID |

**Response** `200 OK`

```json
{
  "id": 1,
  "title": "나의 챗봇 대화방",
  "createdAt": "2026-03-01T10:00:00"
}
```

**Error**

| 상황 | 상태 코드 |
|------|-----------|
| roomId에 해당하는 방이 없는 경우 | `500 Internal Server Error` |

---

### 1-4. 채팅방 삭제

```
DELETE /api/rooms/{roomId}
```

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| roomId | Long | 채팅방 ID |

**Response** `204 No Content`

> 채팅방 삭제 시 해당 방의 모든 Chat 기록도 함께 삭제됩니다. (CascadeType.ALL)

**Error**

| 상황 | 상태 코드 |
|------|-----------|
| roomId에 해당하는 방이 없는 경우 | `500 Internal Server Error` |

---

## 2. Chat API

특정 채팅방의 대화 기록을 저장하고 조회하는 API입니다.  
모든 엔드포인트는 `/api/rooms/{roomId}/chats` 하위에 위치합니다.

### Role 타입

| 값 | 설명 |
|----|------|
| `USER` | 사용자가 입력한 메시지 |
| `ASSISTANT` | 챗봇이 응답한 메시지 |

---

### 2-1. 대화 저장

```
POST /api/rooms/{roomId}/chats
```

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| roomId | Long | 채팅방 ID |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| role | String | ✅ | `USER` 또는 `ASSISTANT` |
| message | String | ✅ | 대화 내용 |

**Request Example**
```json
{
  "role": "USER",
  "message": "안녕하세요, 오늘 날씨 어때?"
}
```

**Response** `201 Created`

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long | 대화 ID (자동 증가) |
| roomId | Long | 채팅방 ID |
| role | String | `USER` 또는 `ASSISTANT` |
| message | String | 대화 내용 |
| createdAt | String | 생성 시각 (ISO 8601) |

**Response Example**
```json
{
  "id": 1,
  "roomId": 1,
  "role": "USER",
  "message": "안녕하세요, 오늘 날씨 어때?",
  "createdAt": "2026-03-01T10:05:00"
}
```

**Error**

| 상황 | 상태 코드 |
|------|-----------|
| roomId에 해당하는 방이 없는 경우 | `500 Internal Server Error` |

---

### 2-2. 대화 목록 조회

```
GET /api/rooms/{roomId}/chats
```

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| roomId | Long | 채팅방 ID |

**Response** `200 OK`

> 생성 시각 오름차순(ASC)으로 정렬하여 반환합니다.

```json
[
  {
    "id": 1,
    "roomId": 1,
    "role": "USER",
    "message": "안녕하세요, 오늘 날씨 어때?",
    "createdAt": "2026-03-01T10:05:00"
  },
  {
    "id": 2,
    "roomId": 1,
    "role": "ASSISTANT",
    "message": "오늘은 맑고 따뜻한 날씨입니다.",
    "createdAt": "2026-03-01T10:05:01"
  }
]
```

**Error**

| 상황 | 상태 코드 |
|------|-----------|
| roomId에 해당하는 방이 없는 경우 | `500 Internal Server Error` |

---

### 2-3. 대화 단건 조회

```
GET /api/rooms/{roomId}/chats/{chatId}
```

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| roomId | Long | 채팅방 ID |
| chatId | Long | 대화 ID |

**Response** `200 OK`

```json
{
  "id": 1,
  "roomId": 1,
  "role": "USER",
  "message": "안녕하세요, 오늘 날씨 어때?",
  "createdAt": "2026-03-01T10:05:00"
}
```

**Error**

| 상황 | 상태 코드 |
|------|-----------|
| chatId에 해당하는 대화가 없는 경우 | `500 Internal Server Error` |

---

## 3. curl 예시

### 채팅방 생성
```bash
curl -X POST http://localhost:8080/api/rooms \
  -H "Content-Type: application/json" \
  -d '{"title": "나의 챗봇 대화방"}'
```

### 전체 채팅방 조회
```bash
curl http://localhost:8080/api/rooms
```

### 채팅방 단건 조회
```bash
curl http://localhost:8080/api/rooms/1
```

### 채팅방 삭제
```bash
curl -X DELETE http://localhost:8080/api/rooms/1
```

### 대화 저장 (사용자 메시지)
```bash
curl -X POST http://localhost:8080/api/rooms/1/chats \
  -H "Content-Type: application/json" \
  -d '{"role": "USER", "message": "안녕하세요, 오늘 날씨 어때?"}'
```

### 대화 저장 (챗봇 응답)
```bash
curl -X POST http://localhost:8080/api/rooms/1/chats \
  -H "Content-Type: application/json" \
  -d '{"role": "ASSISTANT", "message": "오늘은 맑고 따뜻한 날씨입니다."}'
```

### 대화 목록 조회
```bash
curl http://localhost:8080/api/rooms/1/chats
```

### 대화 단건 조회
```bash
curl http://localhost:8080/api/rooms/1/chats/1
```
