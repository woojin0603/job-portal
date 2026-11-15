package kr.co.jobhub.common.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class SafeExternalUrlTest {
    private final SafeExternalUrl urls = new SafeExternalUrl();

    @Test
    void rejectsNonHttpsAndLocalTargetsBeforeServerFetch() {
        assertThrows(IllegalArgumentException.class,
                () -> urls.requirePublicHttps("http://example.com/file.pdf"));
        assertThrows(IllegalArgumentException.class,
                () -> urls.requirePublicHttps("https://localhost/internal"));
        assertThrows(IllegalArgumentException.class,
                () -> urls.requirePublicHttps("https://127.0.0.1/internal"));
        assertThrows(IllegalArgumentException.class,
                () -> urls.requirePublicHttps("https://user:password@example.com/file"));
    }
}
