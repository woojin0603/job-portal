package kr.co.jobhub.posting.service;

/**
 * 공공기관의 중앙·지방 소속을 근무 형태와 분리해 일관되게 해석한다.
 * ALIO 기관코드가 있는 공고는 중앙정부 공공기관 공시 자료이므로 중앙으로 분류한다.
 */
public final class PublicInstitutionClassifier {
    public static final String CENTRAL_PUBLIC = "CENTRAL_PUBLIC";
    public static final String LOCAL_PUBLIC = "LOCAL_PUBLIC";

    private PublicInstitutionClassifier() {
    }

    public static String resolve(String organizationType, String storedType,
                                 String source, String alioInstitutionCode) {
        if (!"PUBLIC".equals(organizationType)) return null;
        // 과거 버전이 근무 형태로 LOCAL_PUBLIC을 저장했더라도 ALIO 근거가 있으면 중앙 분류를 우선한다.
        if (hasText(alioInstitutionCode) || isAlioSource(source)) {
            return CENTRAL_PUBLIC;
        }
        if (CENTRAL_PUBLIC.equals(storedType) || LOCAL_PUBLIC.equals(storedType)) return storedType;
        return null;
    }

    public static String fromConfiguredSource(String organizationType, String configuredType) {
        if (!"PUBLIC".equals(organizationType)) return null;
        return CENTRAL_PUBLIC.equals(configuredType) || LOCAL_PUBLIC.equals(configuredType)
                ? configuredType : null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean isAlioSource(String source) {
        return "PUBLIC-DATA-RECRUITMENT".equals(source) || "JOB-ALIO".equals(source);
    }
}
