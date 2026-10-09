package kr.co.jobhub.posting.service;

import kr.co.jobhub.posting.domain.JobPosting;
import kr.co.jobhub.posting.domain.PostingChange;
import kr.co.jobhub.posting.domain.RecruitmentPosition;
import kr.co.jobhub.posting.repository.PostingChangeRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostingChangeServiceTest {
    @Test
    void storesOnlyChangedFieldsForAnExistingPosting() {
        List<PostingChange> saved = new ArrayList<>();
        PostingChangeRepository repository = repository(saved);
        PostingChangeService service = new PostingChangeService(repository);
        JobPosting posting = posting();
        RecruitmentPosition beforePosition = position("행정", 2, "학력 무관");
        PostingChangeService.Snapshot before = service.snapshot(posting, List.of(beforePosition));

        posting.deadline = LocalDate.of(2026, 10, 31);
        RecruitmentPosition afterPosition = position("행정", 3, "학력 무관");
        service.detect(before, posting, List.of(afterPosition));

        assertEquals(2, saved.size());
        assertTrue(saved.stream().anyMatch(change -> isChange(change, "DEADLINE", "2026-10-20", "2026-10-31")));
        assertTrue(saved.stream().anyMatch(change -> isChange(change, "HEADCOUNT", "행정: 2", "행정: 3")));
        assertTrue(saved.stream().anyMatch(change -> "DEADLINE".equals(change.changeType)
                && "NORMAL".equals(change.importance)));
        assertTrue(saved.stream().anyMatch(change -> "HEADCOUNT".equals(change.changeType)
                && "IMPORTANT".equals(change.importance)));
    }

    @Test
    void ignoresNewAndUnchangedPostings() {
        List<PostingChange> saved = new ArrayList<>();
        PostingChangeRepository repository = repository(saved);
        PostingChangeService service = new PostingChangeService(repository);
        JobPosting posting = posting();
        RecruitmentPosition position = position("전산", 1, "관련 자격증");

        service.detect(null, posting, List.of(position));
        PostingChangeService.Snapshot before = service.snapshot(posting, List.of(position));
        service.detect(before, posting, List.of(position));

        assertTrue(saved.isEmpty());
    }

    @Test
    void ignoresFormattingListOrderAndTrackingUrlNoise() {
        List<PostingChange> saved = new ArrayList<>();
        PostingChangeRepository repository = repository(saved);
        PostingChangeService service = new PostingChangeService(repository);
        JobPosting posting = posting();
        posting.region = "서울, 경기";
        posting.employmentType = "정규직 / 계약직";
        posting.sourceUrl = "https://EXAMPLE.org:443/jobs/7/?id=10&utm_source=newsletter#top";
        RecruitmentPosition admin = position("행정", 2, "학력  무관");
        RecruitmentPosition tech = position("전산", 1, "관련 자격증");
        PostingChangeService.Snapshot before = service.snapshot(posting, List.of(admin, tech));

        posting.region = "경기 / 서울";
        posting.employmentType = "계약직,정규직";
        posting.sourceUrl = "https://example.org/jobs/7?id=10&fbclid=temporary";
        admin.requirements = "학력\u00a0무관";
        service.detect(before, posting, List.of(tech, admin));

        assertTrue(saved.isEmpty());
    }

    @Test
    void treatsDeadlineExtensionAndTitleEditAsNormal() {
        List<PostingChange> saved = new ArrayList<>();
        PostingChangeService service = new PostingChangeService(repository(saved));
        JobPosting posting = posting();
        PostingChangeService.Snapshot before = service.snapshot(posting, List.of());

        posting.deadline = LocalDate.of(2026, 10, 25);
        posting.title = "2026년 직원 공개채용";
        service.detect(before, posting, List.of());

        assertEquals(2, saved.size());
        assertTrue(saved.stream().allMatch(change -> "NORMAL".equals(change.importance)));
    }

    private JobPosting posting() {
        JobPosting posting = new JobPosting();
        posting.id = 7L;
        posting.title = "2026년 직원 채용";
        posting.region = "서울";
        posting.employmentType = "정규직";
        posting.deadline = LocalDate.of(2026, 10, 20);
        posting.sourceUrl = "https://example.org/jobs/7";
        return posting;
    }

    private RecruitmentPosition position(String name, int headcount, String requirements) {
        RecruitmentPosition position = new RecruitmentPosition();
        position.originalName = name;
        position.standardCategory = name;
        position.headcount = headcount;
        position.requirements = requirements;
        return position;
    }

    private boolean isChange(PostingChange change, String type, String oldValue, String newValue) {
        return type.equals(change.changeType)
                && oldValue.equals(change.oldValue)
                && newValue.equals(change.newValue);
    }

    private PostingChangeRepository repository(List<PostingChange> saved) {
        return (PostingChangeRepository) Proxy.newProxyInstance(
                PostingChangeRepository.class.getClassLoader(),
                new Class<?>[]{PostingChangeRepository.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("save")) {
                        saved.add((PostingChange) arguments[0]);
                        return arguments[0];
                    }
                    if (method.getReturnType().equals(boolean.class)) return false;
                    if (method.getReturnType().equals(long.class)) return 0L;
                    return null;
                });
    }
}
