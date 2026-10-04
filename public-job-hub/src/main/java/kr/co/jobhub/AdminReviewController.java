package kr.co.jobhub;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import kr.co.jobhub.model.JobPosting;
import kr.co.jobhub.model.RecruitmentPosition;
import kr.co.jobhub.repo.JobPostingRepository;
import kr.co.jobhub.repo.RecruitmentPositionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

/** AI와 크롤러가 추출한 공고 분류를 관리자가 검수하고 정정하는 API다. */
@RestController
@RequestMapping("/api/admin/reviews")
public class AdminReviewController {
    public record PositionInput(@NotBlank @Size(max = 500) String standardCategory,
                                @NotBlank @Size(max = 500) String originalName,
                                Integer headcount, @Size(max = 500) String workRegion,
                                @Size(max = 20000) String requirements) {}
    public record ReviewRequest(@Size(max = 500) String region,
                                @Size(max = 500) String employmentType,
                                String organizationType, String mobilityType,
                                List<@Valid PositionInput> positions) {}
    public record PositionView(Long id, String standardCategory, String originalName, Integer headcount,
                               String workRegion, String requirements) {}
    public record ReviewView(Long id, String title, String organization, String sourceUrl, String region,
                             String employmentType, String organizationType, String publicInstitutionType,
                             String mobilityType, List<PositionView> positions, Instant updatedAt) {}

    private final JobPostingRepository postings;
    private final RecruitmentPositionRepository positions;

    public AdminReviewController(JobPostingRepository postings, RecruitmentPositionRepository positions) {
        this.postings = postings;
        this.positions = positions;
    }

    @GetMapping
    public List<ReviewView> list(@RequestParam(defaultValue = "0") int page) {
        return postings.findAll(PageRequest.of(Math.max(0, page), 30, Sort.by(Sort.Direction.DESC, "updatedAt")))
                .stream().map(this::view).toList();
    }

    @PostMapping("/{id}")
    @Transactional
    public ReviewView update(@PathVariable Long id, @Valid @RequestBody ReviewRequest request) {
        JobPosting posting = postings.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "공고를 찾을 수 없습니다."));
        posting.region = clean(request.region());
        posting.employmentType = clean(request.employmentType());
        posting.organizationType = allowed(request.organizationType(), List.of("PUBLIC", "PRIVATE"), "기관 유형");
        posting.mobilityType = allowed(request.mobilityType(), List.of("ROTATIONAL", "FIXED", "UNKNOWN"), "근무 형태");
        posting.publicInstitutionType = "PUBLIC".equals(posting.organizationType)
                ? "ROTATIONAL".equals(posting.mobilityType) ? "CENTRAL_PUBLIC"
                : "FIXED".equals(posting.mobilityType) ? "LOCAL_PUBLIC" : null : null;
        posting.updatedAt = Instant.now();
        postings.save(posting);
        positions.deleteByPostingId(id);
        if (request.positions() != null) for (PositionInput input : request.positions()) {
            RecruitmentPosition position = new RecruitmentPosition();
            position.posting = posting;
            position.standardCategory = input.standardCategory().trim();
            position.originalName = input.originalName().trim();
            position.headcount = input.headcount();
            position.workRegion = clean(input.workRegion());
            position.requirements = clean(input.requirements());
            positions.save(position);
        }
        return view(posting);
    }

    private ReviewView view(JobPosting posting) {
        List<PositionView> rows = positions.findByPostingIdOrderById(posting.id).stream()
                .map(p -> new PositionView(p.id, p.standardCategory, p.originalName, p.headcount, p.workRegion, p.requirements)).toList();
        return new ReviewView(posting.id, posting.title, posting.organization, posting.sourceUrl, posting.region,
                posting.employmentType, posting.organizationType, posting.publicInstitutionType,
                posting.mobilityType, rows, posting.updatedAt);
    }

    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String allowed(String value, List<String> values, String label) {
        if (value == null || !values.contains(value)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + "이 올바르지 않습니다.");
        return value;
    }
}
