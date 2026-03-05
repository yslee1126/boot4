package com.example.boot4.primitive;

/**
 * Java 25 Preview: Primitive Types in Patterns (JEP 455) 테스트용 헬퍼
 * switch / instanceof 에서 int, long, float, double 직접 패턴 매칭 실험
 */
public class PrimitivePatternTestHelper {

    // ── 1. switch 에서 primitive 타입 직접 매칭 ──────────────────────────
    public static String classifyNumber(Object value) {
        return switch (value) {
            case Integer i when i < 0 -> "음수 int: " + i;
            case Integer i when i == 0 -> "영(0)";
            case Integer i -> "양수 int: " + i;
            case Long l -> "long 값: " + l;
            case Double d -> "double 값: " + d;
            case Float f -> "float 값: " + f;
            case String s -> "문자열: " + s;
            case null -> "null";
            default -> "알 수 없는 타입";
        };
    }

    // ── 2. instanceof 에서 primitive 패턴 바인딩 (JEP 455 핵심) ─────────
    // Boxing된 Integer를 instanceof int i 로 unboxing 바인딩
    public static String describePrimitive(Object obj) {
        if (obj instanceof int i) {
            return "int: " + i;
        } else if (obj instanceof long l) {
            return "long: " + l;
        } else if (obj instanceof double d) {
            return "double: " + d;
        }
        return "기타: " + obj;
    }

    // ── 3. Record + primitive 복합 패턴 매칭 (복잡한 DTO 파싱 간소화) ───
    public record SensorReading(String type, Object value) {
    }

    public static String parseSensorReading(SensorReading reading) {
        return switch (reading) {
            case SensorReading(var t, Integer i) when i > 100 -> "[" + t + "] 경고: 정수 임계값 초과 → " + i;
            case SensorReading(var t, Integer i) -> "[" + t + "] 정상 정수 값 → " + i;
            case SensorReading(var t, Double d) when d > 37.5 -> "[" + t + "] 경고: 실수 임계값 초과 → " + d;
            case SensorReading(var t, Double d) -> "[" + t + "] 정상 실수 값 → " + d;
            case SensorReading(var t, String s) -> "[" + t + "] 문자열 값 → " + s;
            default -> "알 수 없는 센서 데이터";
        };
    }

    // ── 4. 기존 방식 (if-else + 캐스팅) - 패턴 매칭 결과와 비교용 ───────
    public static String classifyOldStyle(Object value) {
        if (value instanceof Integer) {
            int i = (Integer) value;
            if (i < 0)
                return "음수 int: " + i;
            if (i == 0)
                return "영(0)";
            return "양수 int: " + i;
        } else if (value instanceof Long) {
            return "long 값: " + value;
        } else if (value instanceof Double) {
            return "double 값: " + value;
        } else if (value instanceof Float) {
            return "float 값: " + value;
        } else if (value instanceof String s) {
            return "문자열: " + s;
        } else if (value == null) {
            return "null";
        }
        return "알 수 없는 타입";
    }
}
