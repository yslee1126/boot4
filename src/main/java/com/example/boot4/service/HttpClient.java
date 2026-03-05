package com.example.boot4.service;

import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * 범용 HTTP 클라이언트 인터페이스 (Spring 6+ @HttpExchange)
 * 여러 도메인의 RSS 피드를 가져올 수 있도록 일반화되었습니다.
 */
@HttpExchange
public interface HttpClient {

    /**
     * 토스 기술 블로그 RSS 피드를 가져옵니다.
     */
    @GetExchange("https://toss.tech/rss.xml")
    String getTossRssFeed();

    /**
     * 네이버 D2 기술 블로그 RSS 피드를 가져옵니다.
     */
    @GetExchange("https://d2.naver.com/d2.atom")
    String getD2RssFeed();
}
