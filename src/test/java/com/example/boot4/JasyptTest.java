package com.example.boot4;

import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.junit.jupiter.api.Test;

public class JasyptTest {

    /**
     * 실행 방법:
     * JASYPT_KEY="your_key" ./gradlew test --tests *JasyptTest
     */
    @Test
    void encryptPassword() {
        String key = System.getenv("JASYPT_KEY");
        if (key == null || key.isEmpty()) {
            throw new RuntimeException("JASYPT_KEY 환경 변수가 필요합니다.");
        }

        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword(key); // 환경 변수에서 키 읽기
        config.setAlgorithm("PBEWITHHMACSHA512ANDAES_256");
        config.setKeyObtentionIterations("1000");
        config.setPoolSize("1");
        config.setProviderName("SunJCE");
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
        config.setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator");
        config.setStringOutputType("base64");
        encryptor.setConfig(config);

        String plainText = "admin123";
        String encrypted = encryptor.encrypt(plainText);
        System.out.println("PLAIN: " + plainText);
        System.out.println("ENCRYPTED: ENC(" + encrypted + ")");
    }
}
