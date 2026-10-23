package kr.co.jobhub.collection.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecruitmentScheduleScannerTest {
    private final RecruitmentScheduleScanner scanner =
            new RecruitmentScheduleScanner(null, null, null, null, null);

    @Test
    void extractsStagesWhetherDateComesBeforeOrAfterLabel() {
        String notice = "서류전형 합격자 발표 2026.11.03 / "
                + "2026년 11월 10일 면접전형 / 최종 합격 발표 2026-11-20";
        assertThat(scanner.extractedSummary(notice)).containsExactly(
                "DOCUMENT_RESULT|2026-11-03|false",
                "INTERVIEW|2026-11-10|false",
                "FINAL_RESULT|2026-11-20|false");
    }

    @Test
    void marksTentativeSchedule() {
        assertThat(scanner.extractedSummary("일정 변동 가능: 임용 예정일 2026.12.01"))
                .containsExactly("APPOINTMENT|2026-12-01|true");
    }

    @Test
    void ignoresDatesWithoutRecruitmentStage() {
        assertThat(scanner.extractedSummary("공고 게시일 2026.10.24, 문의전화 02-1234-5678"))
                .isEmpty();
    }
}