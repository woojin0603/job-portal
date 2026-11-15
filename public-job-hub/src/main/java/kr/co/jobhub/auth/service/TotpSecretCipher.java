package kr.co.jobhub.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/** DB 유출만으로 관리자 TOTP 비밀키가 노출되지 않도록 애플리케이션 외부 키로 암호화한다. */
@Service
public class TotpSecretCipher {
    private static final String PREFIX = "enc:v1:";
    private static final SecureRandom RANDOM = new SecureRandom();
    private final byte[] key;
    public TotpSecretCipher(@Value("${jobhub.security.totp-encryption-key:}") String encodedKey) {
        if (encodedKey == null || encodedKey.isBlank()) { this.key = null; return; }
        try {
            byte[] decoded = Base64.getDecoder().decode(encodedKey.trim());
            if (decoded.length != 32) throw new IllegalArgumentException();
            this.key = decoded;
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("JOBHUB_TOTP_ENCRYPTION_KEY는 Base64 형식의 32바이트 키여야 합니다.");
        }
    }
    public String encrypt(String secret) {
        if (key == null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "관리자 2단계 인증 암호화 키가 설정되지 않았습니다.");
        try {
            byte[] nonce = new byte[12]; RANDOM.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
            byte[] encrypted = cipher.doFinal(secret.getBytes(StandardCharsets.UTF_8));
            byte[] packed = new byte[nonce.length + encrypted.length];
            System.arraycopy(nonce, 0, packed, 0, nonce.length);
            System.arraycopy(encrypted, 0, packed, nonce.length, encrypted.length);
            return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(packed);
        } catch (Exception exception) { throw new IllegalStateException("TOTP 비밀키를 암호화하지 못했습니다.", exception); }
    }
    public String decrypt(String stored) {
        if (stored == null || stored.isBlank() || !stored.startsWith(PREFIX)) return stored;
        if (key == null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "관리자 2단계 인증 암호화 키를 읽을 수 없습니다.");
        try {
            byte[] packed = Base64.getUrlDecoder().decode(stored.substring(PREFIX.length()));
            if (packed.length <= 28) throw new IllegalArgumentException();
            byte[] nonce = java.util.Arrays.copyOfRange(packed, 0, 12);
            byte[] encrypted = java.util.Arrays.copyOfRange(packed, 12, packed.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception exception) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "관리자 2단계 인증 비밀키를 복호화하지 못했습니다."); }
    }
    public boolean encrypted(String value) { return value != null && value.startsWith(PREFIX); }
    public boolean available() { return key != null; }
}
