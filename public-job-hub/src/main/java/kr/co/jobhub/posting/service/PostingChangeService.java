package kr.co.jobhub.posting.service;

import kr.co.jobhub.posting.domain.*;
import kr.co.jobhub.posting.repository.PostingChangeRepository;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.text.Normalizer;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 기존 공고와 재수집 결과를 비교해 사용자에게 의미 있는 변경만 저장한다. */
@Service
public class PostingChangeService {
    public record Snapshot(String title, String region, String employmentType, LocalDate deadline,
                           String sourceUrl, String headcounts, String requirements, String positions) {}

    private final PostingChangeRepository changes;

    public PostingChangeService(PostingChangeRepository changes) {
        this.changes = changes;
    }

    public Snapshot snapshot(JobPosting posting, List<RecruitmentPosition> positions) {
        if (posting == null || posting.id == null) return null;
        return new Snapshot(clean(posting.title), clean(posting.region), clean(posting.employmentType),
                posting.deadline, clean(posting.sourceUrl), positionValue(positions, p -> value(p.headcount)),
                positionValue(positions, p -> clean(p.requirements)),
                positionValue(positions, p -> clean(p.standardCategory) + " / " + clean(p.originalName)));
    }

    public void detect(Snapshot before, JobPosting after, List<RecruitmentPosition> current) {
        if (before == null) return;
        Snapshot next = snapshot(after, current);
        Instant detectedAt = Instant.now();
        compare(after, detectedAt, "TITLE", "공고 제목", before.title(), next.title());
        compare(after, detectedAt, "DEADLINE", "지원 마감일", value(before.deadline()), value(next.deadline()));
        compare(after, detectedAt, "REGION", "근무 지역", before.region(), next.region());
        compare(after, detectedAt, "EMPLOYMENT_TYPE", "고용 형태", before.employmentType(), next.employmentType());
        compare(after, detectedAt, "SOURCE_URL", "원문·첨부 링크", before.sourceUrl(), next.sourceUrl());
        compare(after, detectedAt, "HEADCOUNT", "채용 인원", before.headcounts(), next.headcounts());
        compare(after, detectedAt, "REQUIREMENTS", "지원 자격", before.requirements(), next.requirements());
        compare(after, detectedAt, "POSITIONS", "채용 직렬", before.positions(), next.positions());
    }

    private void compare(JobPosting posting, Instant detectedAt, String type, String label,
                         String oldValue, String newValue) {
        if (equivalent(type, oldValue, newValue)) return;
        PostingChange change = new PostingChange();
        change.posting = posting;
        change.changeType = type;
        change.fieldLabel = label;
        change.importance = importance(type, oldValue, newValue);
        change.oldValue = limit(oldValue);
        change.newValue = limit(newValue);
        change.sourceUrl = posting.sourceUrl;
        change.detectedAt = detectedAt;
        changes.save(change);
    }

    private String importance(String type, String oldValue, String newValue) {
        if (Set.of("REQUIREMENTS", "HEADCOUNT", "POSITIONS", "REGION", "EMPLOYMENT_TYPE").contains(type)) {
            return "IMPORTANT";
        }
        if ("DEADLINE".equals(type)) {
            try {
                LocalDate oldDate = LocalDate.parse(clean(oldValue));
                LocalDate newDate = LocalDate.parse(clean(newValue));
                return newDate.isBefore(oldDate) ? "IMPORTANT" : "NORMAL";
            } catch (Exception ignored) {
                return "IMPORTANT";
            }
        }
        return "NORMAL";
    }

    private String positionValue(List<RecruitmentPosition> positions,
                                 Function<RecruitmentPosition, String> mapper) {
        return positions.stream().map(position -> clean(position.originalName) + ": " + mapper.apply(position))
                .filter(value -> !value.endsWith(": "))
                .sorted(String.CASE_INSENSITIVE_ORDER).distinct().collect(Collectors.joining("\n"));
    }

    private boolean equivalent(String type, String oldValue, String newValue) {
        return Objects.equals(comparable(type, oldValue), comparable(type, newValue));
    }

    private String comparable(String type, String value) {
        if ("SOURCE_URL".equals(type)) return canonicalUrl(value);
        if ("REGION".equals(type) || "EMPLOYMENT_TYPE".equals(type)) return canonicalList(value);
        return clean(value)
                .replaceAll("\\s*([,:;|/·ㆍ])\\s*", "$1")
                .replaceAll("[‐‑‒–—―]", "-");
    }

    private String canonicalList(String value) {
        return Arrays.stream(clean(value).split("[,\\n/|·ㆍ]+"))
                .map(this::clean).filter(item -> !item.isBlank())
                .collect(Collectors.toCollection(() -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)))
                .stream().collect(Collectors.joining("|"));
    }

    /** 광고·분석용 쿼리와 fragment 차이 때문에 같은 원문이 변경으로 기록되지 않게 한다. */
    private String canonicalUrl(String value) {
        String cleaned = clean(value);
        if (cleaned.isBlank()) return "";
        try {
            URI uri = URI.create(cleaned);
            List<String> query = uri.getRawQuery() == null ? new ArrayList<>()
                    : Arrays.stream(uri.getRawQuery().split("&"))
                    .filter(part -> !isTrackingParameter(part)).sorted().toList();
            String path = Optional.ofNullable(uri.getRawPath()).orElse("").replaceAll("/$", "");
            int port = uri.getPort();
            if (("https".equalsIgnoreCase(uri.getScheme()) && port == 443)
                    || ("http".equalsIgnoreCase(uri.getScheme()) && port == 80)) port = -1;
            return new URI(uri.getScheme() == null ? null : uri.getScheme().toLowerCase(Locale.ROOT),
                    uri.getRawUserInfo(), uri.getHost() == null ? null : uri.getHost().toLowerCase(Locale.ROOT),
                    port, path, query.isEmpty() ? null : String.join("&", query), null).toString();
        } catch (Exception ignored) {
            return cleaned.replaceFirst("#.*$", "");
        }
    }

    private boolean isTrackingParameter(String part) {
        String key = part.split("=", 2)[0].toLowerCase(Locale.ROOT);
        return key.startsWith("utm_") || Set.of("fbclid", "gclid", "dclid", "yclid", "ref", "source")
                .contains(key);
    }

    private String clean(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .replace('\u00a0', ' ').replaceAll("[\\u200B-\\u200D\\uFEFF]", "")
                .trim().replaceAll("\\s+", " ");
    }
    private String value(Object value) { return value == null ? "" : String.valueOf(value); }
    private String limit(String value) {
        String clean = clean(value);
        return clean.length() <= 10000 ? clean : clean.substring(0, 10000) + "…";
    }
}
