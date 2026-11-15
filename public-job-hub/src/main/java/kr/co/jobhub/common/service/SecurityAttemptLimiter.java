package kr.co.jobhub.common.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 인증 관련 공개·고위험 요청의 짧은 폭주를 서버 전체에서 제한한다. */
@Service
public class SecurityAttemptLimiter {
    private final Map<String, ArrayDeque<Instant>> attempts = new ConcurrentHashMap<>();

    public void require(String scope, String subject, int maximum, Duration window) {
        String key = key(scope, subject);
        Instant cutoff = Instant.now().minus(window);
        ArrayDeque<Instant> values = attempts.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (values) {
            while (!values.isEmpty() && values.peekFirst().isBefore(cutoff)) values.removeFirst();
            if (values.size() >= maximum) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.");
            }
            values.addLast(Instant.now());
        }
        if (attempts.size() > 20_000) attempts.entrySet().removeIf(entry -> {
            ArrayDeque<Instant> queue = entry.getValue();
            synchronized (queue) { return queue.isEmpty() || queue.peekLast().isBefore(cutoff); }
        });
    }

    /** 검증에 성공한 계정은 실패 횟수를 즉시 비워 정상 사용이 제한되지 않게 한다. */
    public void clear(String scope, String subject) {
        attempts.remove(key(scope, subject));
    }

    private String key(String scope, String subject) {
        String safeSubject = subject == null || subject.isBlank() ? "unknown" : subject.trim().toLowerCase();
        return scope + "|" + safeSubject.substring(0, Math.min(300, safeSubject.length()));
    }
}
