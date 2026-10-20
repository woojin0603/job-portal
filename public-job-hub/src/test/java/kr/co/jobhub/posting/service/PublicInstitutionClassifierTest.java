package kr.co.jobhub.posting.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PublicInstitutionClassifierTest {
    @Test
    void alioPostingIsCentralRegardlessOfMobility() {
        assertThat(PublicInstitutionClassifier.resolve(
                "PUBLIC", null, "PUBLIC-DATA-RECRUITMENT", "C0387"))
                .isEqualTo("CENTRAL_PUBLIC");
    }

    @Test
    void configuredLocalPublicSourceStaysLocal() {
        assertThat(PublicInstitutionClassifier.fromConfiguredSource("PUBLIC", "LOCAL_PUBLIC"))
                .isEqualTo("LOCAL_PUBLIC");
    }

    @Test
    void alioEvidenceOverridesLegacyLocalClassification() {
        assertThat(PublicInstitutionClassifier.resolve(
                "PUBLIC", "LOCAL_PUBLIC", "JOB-ALIO", null))
                .isEqualTo("CENTRAL_PUBLIC");
    }

    @Test
    void privateCompanyHasNoPublicInstitutionScope() {
        assertThat(PublicInstitutionClassifier.resolve(
                "PRIVATE", "CENTRAL_PUBLIC", "PRIVATE-SOURCE", null))
                .isNull();
    }
}
