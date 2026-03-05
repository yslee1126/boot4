package com.example.boot4.primitive;

import com.example.boot4.primitive.PrimitivePatternTestHelper.SensorReading;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Java 25 Preview: Primitive Types in Patterns (JEP 455) 테스트
 * --enable-preview 플래그는 build.gradle에 이미 설정되어 있음
 */
@DisplayName("Primitive Types in Patterns (Java 25 Preview)")
class PrimitivePatternTest {

    @Nested
    @DisplayName("1. switch 문에서 primitive 타입 패턴 매칭")
    class SwitchPrimitivePattern {

        @Test
        @DisplayName("Integer - 음수/0/양수 분기")
        void testIntegerClassification() {
            assertThat(PrimitivePatternTestHelper.classifyNumber(-5))
                    .isEqualTo("음수 int: -5");
            assertThat(PrimitivePatternTestHelper.classifyNumber(0))
                    .isEqualTo("영(0)");
            assertThat(PrimitivePatternTestHelper.classifyNumber(42))
                    .isEqualTo("양수 int: 42");
        }

        @Test
        @DisplayName("Long / Double / Float 분기")
        void testLongDoubleFloat() {
            assertThat(PrimitivePatternTestHelper.classifyNumber(9999L))
                    .isEqualTo("long 값: 9999");
            assertThat(PrimitivePatternTestHelper.classifyNumber(3.14))
                    .isEqualTo("double 값: 3.14");
            assertThat(PrimitivePatternTestHelper.classifyNumber(1.5f))
                    .isEqualTo("float 값: 1.5");
        }

        @Test
        @DisplayName("null 처리 - case null로 NPE 없이 분기")
        void testNullHandling() {
            assertThat(PrimitivePatternTestHelper.classifyNumber(null))
                    .isEqualTo("null");
        }
    }

    @Nested
    @DisplayName("2. instanceof에서 primitive 패턴 바인딩 (JEP 455 핵심)")
    class InstanceofPrimitivePattern {

        @Test
        @DisplayName("Boxing된 값을 instanceof 에서 primitive로 직접 바인딩")
        void testPrimitiveInstanceof() {
            assertThat(PrimitivePatternTestHelper.describePrimitive(100))
                    .isEqualTo("int: 100");
            assertThat(PrimitivePatternTestHelper.describePrimitive(200L))
                    .isEqualTo("long: 200");
            assertThat(PrimitivePatternTestHelper.describePrimitive(3.14))
                    .isEqualTo("double: 3.14");
        }
    }

    @Nested
    @DisplayName("3. Record + Primitive 복합 패턴 매칭 (DTO 파싱 간소화)")
    class RecordWithPrimitivePattern {

        @Test
        @DisplayName("정수 임계값 초과 → 경고 메시지")
        void testSensorOverThreshold() {
            var reading = new SensorReading("온도", 150);
            assertThat(PrimitivePatternTestHelper.parseSensorReading(reading))
                    .contains("경고").contains("150");
        }

        @Test
        @DisplayName("정상 정수 값 처리")
        void testSensorNormalInt() {
            var reading = new SensorReading("압력", 80);
            assertThat(PrimitivePatternTestHelper.parseSensorReading(reading))
                    .contains("정상").contains("80");
        }

        @Test
        @DisplayName("실수 임계값 초과 → 경고 메시지")
        void testSensorDoubleOverThreshold() {
            var reading = new SensorReading("체온", 38.5);
            assertThat(PrimitivePatternTestHelper.parseSensorReading(reading))
                    .contains("경고").contains("38.5");
        }

        @Test
        @DisplayName("정상 실수 값 처리")
        void testSensorNormalDouble() {
            var reading = new SensorReading("습도", 36.0);
            assertThat(PrimitivePatternTestHelper.parseSensorReading(reading))
                    .contains("정상").contains("36.0");
        }
    }

    @Nested
    @DisplayName("4. 기존 if-else 방식 vs 패턴 매칭 결과 동일성 검증")
    class OldVsNewPatternComparison {

        @Test
        @DisplayName("switch 패턴 결과 == 기존 if-else 결과 (동치 검증)")
        void testResultEquality() {
            Object[] testValues = { -10, 0, 42, 9999L, 3.14, "hello", null };

            for (Object val : testValues) {
                assertThat(PrimitivePatternTestHelper.classifyNumber(val))
                        .as("값 [%s] 처리 결과가 동일해야 함", val)
                        .isEqualTo(PrimitivePatternTestHelper.classifyOldStyle(val));
            }
        }
    }
}
