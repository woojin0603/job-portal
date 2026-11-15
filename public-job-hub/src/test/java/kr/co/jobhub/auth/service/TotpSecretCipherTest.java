package kr.co.jobhub.auth.service;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TotpSecretCipherTest {
    private final String key = Base64.getEncoder().encodeToString(new byte[32]);

    @Test
    void encryptsWithRandomNonceAndDecrypts() {
        TotpSecretCipher cipher = new TotpSecretCipher(key);
        String first = cipher.encrypt("JBSWY3DPEHPK3PXP");
        String second = cipher.encrypt("JBSWY3DPEHPK3PXP");
        assertThat(first).startsWith("enc:v1:").isNotEqualTo(second);
        assertThat(cipher.decrypt(first)).isEqualTo("JBSWY3DPEHPK3PXP");
    }

    @Test
    void rejectsInvalidKeyLength() {
        assertThatThrownBy(() -> new TotpSecretCipher(Base64.getEncoder().encodeToString(new byte[16])))
                .isInstanceOf(IllegalStateException.class);
    }
}
