package kr.co.jobhub.auth.service;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

/** 관리자 2단계 인증에 사용하는 Base32 비밀키와 6자리 TOTP 코드를 처리한다. */
@Service
public class TotpService {
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final long STEP_MILLIS = 30_000L;

    public String secret() {
        byte[] bytes = new byte[20];
        SECURE_RANDOM.nextBytes(bytes);
        StringBuilder encoded = new StringBuilder(32);
        int buffer = 0;
        int bits = 0;
        for (byte value : bytes) {
            buffer = (buffer << 8) | (value & 0xff);
            bits += 8;
            while (bits >= 5) {
                bits -= 5;
                encoded.append(ALPHABET.charAt((buffer >> bits) & 31));
            }
        }
        return encoded.toString();
    }

    public boolean verify(String secret, String value) {
        if (secret == null || value == null || !value.matches("\\d{6}")) return false;
        long currentStep = System.currentTimeMillis() / STEP_MILLIS;
        for (long offset = -1; offset <= 1; offset++) {
            if (value.equals(code(secret, currentStep + offset))) return true;
        }
        return false;
    }

    private String code(String secret, long counter) {
        try {
            byte[] message = new byte[8];
            for (int index = message.length - 1; index >= 0; index--) {
                message[index] = (byte) counter;
                counter >>>= 8;
            }
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(decode(secret), "HmacSHA1"));
            byte[] hash = mac.doFinal(message);
            int offset = hash[hash.length - 1] & 15;
            int number = ((hash[offset] & 127) << 24)
                    | ((hash[offset + 1] & 255) << 16)
                    | ((hash[offset + 2] & 255) << 8)
                    | (hash[offset + 3] & 255);
            return String.format("%06d", number % 1_000_000);
        } catch (GeneralSecurityException exception) {
            return "";
        }
    }

    private byte[] decode(String value) {
        int buffer = 0;
        int bits = 0;
        int index = 0;
        byte[] decoded = new byte[value.length() * 5 / 8];
        for (char character : value.toCharArray()) {
            int digit = ALPHABET.indexOf(character);
            if (digit < 0) continue;
            buffer = (buffer << 5) | digit;
            bits += 5;
            if (bits >= 8) {
                bits -= 8;
                decoded[index++] = (byte) (buffer >> bits);
            }
        }
        return decoded;
    }
}
