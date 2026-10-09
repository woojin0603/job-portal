package kr.co.jobhub.posting.controller;

import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import kr.co.jobhub.posting.domain.PostingChange;
import kr.co.jobhub.posting.repository.PostingChangeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 관리자가 변경 감지 결과를 검색하고 오탐 이력을 정리하는 API다. */
@RestController
@RequestMapping("/api/admin/posting-changes")
public class AdminPostingChangeController {
    public record ChangeView(Long id, Long postingId, String title, String organization,
                             String changeType, String fieldLabel, String importance, String oldValue, String newValue,
                             String sourceUrl, Instant detectedAt) {}
    public record ChangePage(List<ChangeView> items, long total, int page, int totalPages) {}
    public record DeleteRequest(@NotEmpty List<Long> ids) {}
    public record DeleteResult(long deleted) {}

    private final PostingChangeRepository changes;

    public AdminPostingChangeController(PostingChangeRepository changes) {
        this.changes = changes;
    }

    @GetMapping
    public ChangePage list(@RequestParam(defaultValue = "") String q,
                           @RequestParam(defaultValue = "") String type,
                           @RequestParam(defaultValue = "") String importance,
                           @RequestParam(defaultValue = "0") int page) {
        String keyword = q.trim().toLowerCase(Locale.ROOT);
        String changeType = type.trim().toUpperCase(Locale.ROOT);
        String importanceType = importance.trim().toUpperCase(Locale.ROOT);
        Specification<PostingChange> filter = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!keyword.isBlank()) {
                var posting = root.join("posting");
                String pattern = "%" + keyword + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(posting.get("title")), pattern),
                        builder.like(builder.lower(posting.get("organization")), pattern),
                        builder.like(builder.lower(root.get("fieldLabel")), pattern)));
            }
            if (!changeType.isBlank()) predicates.add(builder.equal(root.get("changeType"), changeType));
            if ("IMPORTANT".equals(importanceType)) {
                predicates.add(builder.equal(root.get("importance"), "IMPORTANT"));
            } else if ("NORMAL".equals(importanceType)) {
                predicates.add(builder.or(builder.equal(root.get("importance"), "NORMAL"),
                        builder.isNull(root.get("importance"))));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        Page<PostingChange> result = changes.findAll(filter,
                PageRequest.of(Math.max(0, page), 30, Sort.by(Sort.Direction.DESC, "detectedAt")));
        return new ChangePage(result.stream().map(this::view).toList(), result.getTotalElements(),
                result.getNumber(), result.getTotalPages());
    }

    @PostMapping("/delete")
    @Transactional
    public DeleteResult delete(@Valid @RequestBody DeleteRequest request) {
        List<Long> ids = request.ids().stream().filter(java.util.Objects::nonNull).distinct().toList();
        long count = changes.count((root, query, builder) -> root.get("id").in(ids));
        changes.deleteAllByIdInBatch(ids);
        return new DeleteResult(count);
    }

    private ChangeView view(PostingChange change) {
        return new ChangeView(change.id, change.posting.id, change.posting.title, change.posting.organization,
                change.changeType, change.fieldLabel, change.importance == null ? "NORMAL" : change.importance,
                change.oldValue, change.newValue,
                change.sourceUrl, change.detectedAt);
    }
}
