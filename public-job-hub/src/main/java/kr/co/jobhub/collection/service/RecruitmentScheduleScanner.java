package kr.co.jobhub.collection.service;

import kr.co.jobhub.career.domain.RecruitmentSchedule;
import kr.co.jobhub.career.repository.RecruitmentScheduleRepository;
import kr.co.jobhub.career.repository.ScrapRepository;
import kr.co.jobhub.common.service.RobotsPolicy;
import kr.co.jobhub.posting.domain.JobPosting;
import kr.co.jobhub.posting.repository.JobPostingRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 스크랩된 공개 공고의 HTML·텍스트 PDF에서 명시된 전형 날짜만 추출한다. */
@Service
public class RecruitmentScheduleScanner {
    private record StageRule(String type, String label, String words) {}
    private record Found(String type, String label, LocalDate date, boolean tentative, String evidence, String url) {}

    private static final List<StageRule> RULES = List.of(
            new StageRule("DOCUMENT_RESULT", "서류 합격 발표", "서류.{0,12}(합격|결과|발표)"),
            new StageRule("WRITTEN_TEST", "필기시험", "(필기|NCS|직업기초).{0,10}(시험|전형|평가)"),
            new StageRule("INTERVIEW", "면접", "(면접|역량면접|토론면접).{0,10}(시험|전형|평가|심사)?"),
            new StageRule("FINAL_RESULT", "최종 합격 발표", "최종.{0,12}(합격|결과|발표)"),
            new StageRule("APPOINTMENT", "임용 예정", "(임용|입사|출근).{0,10}(예정|일|등록)?")
    );
    private static final Pattern DATE = Pattern.compile(
            "(?<!\\d)(20\\d{2})\\s*(?:[.\\-/]|년)\\s*(\\d{1,2})\\s*(?:[.\\-/]|월)\\s*(\\d{1,2})(?:\\s*일)?(?!\\d)");

    private final ScrapRepository scraps;
    private final JobPostingRepository postings;
    private final RecruitmentScheduleRepository schedules;
    private final RobotsPolicy robots;
    private final CrawlService crawler;

    public RecruitmentScheduleScanner(ScrapRepository scraps, JobPostingRepository postings,
                                      RecruitmentScheduleRepository schedules, RobotsPolicy robots,
                                      CrawlService crawler) {
        this.scraps = scraps; this.postings = postings; this.schedules = schedules;
        this.robots = robots; this.crawler = crawler;
    }

    /** 매시간 스크랩된 진행 공고를 확인한다. 한 번에 제한해 외부 기관에 부담을 주지 않는다. */
    @Scheduled(cron = "0 20 * * * *", zone = "Asia/Seoul")
    public void scanScrappedPostings() {
        scraps.findDistinctOpenPostingIds().stream().limit(40).forEach(id -> {
            try { postings.findById(id).ifPresent(this::scan); } catch (RuntimeException ignored) { }
        });
    }

    public void scan(JobPosting posting) {
        if (posting.sourceUrl == null || posting.sourceUrl.isBlank()) return;
        try {
            URI pageUri = URI.create(posting.sourceUrl);
            if (!"https".equalsIgnoreCase(pageUri.getScheme()) || pageUri.getHost() == null || !robots.allows(pageUri)) return;
            Document page = Jsoup.connect(posting.sourceUrl).userAgent("PublicJobHub/0.7")
                    .timeout(15_000).maxBodySize(4_000_000).get();
            List<Found> found = new ArrayList<>(extract(page.text(), posting.sourceUrl));
            String pdfUrl = pdfUrl(page, pageUri);
            if (pdfUrl != null) {
                try {
                    byte[] bytes = Jsoup.connect(pdfUrl).userAgent("PublicJobHub/0.7").ignoreContentType(true)
                            .timeout(20_000).maxBodySize(20_000_000).execute().bodyAsBytes();
                    if (bytes.length > 4 && bytes.length <= 20_000_000)
                        found.addAll(extract(crawler.extractPdfDocument(bytes), pdfUrl));
                } catch (Exception ignored) { }
            }
            replace(posting, found);
        } catch (Exception ignored) {
            // 외부 사이트 장애는 기존에 확인한 일정을 지우지 않는다.
        }
    }

    List<Found> extract(String raw, String sourceUrl) {
        if (raw == null || raw.isBlank()) return List.of();
        String text = raw.replaceAll("[\\r\\n\\t]+", " ").replaceAll("\\s+", " ");
        Map<String, Found> result = new LinkedHashMap<>();
        Matcher dateMatcher = DATE.matcher(text);
        while (dateMatcher.find()) {
            try {
                LocalDate date = LocalDate.of(Integer.parseInt(dateMatcher.group(1)),
                        Integer.parseInt(dateMatcher.group(2)), Integer.parseInt(dateMatcher.group(3)));
                if (date.isBefore(LocalDate.now().minusYears(1)) || date.isAfter(LocalDate.now().plusYears(3))) continue;
                int contextStart = Math.max(0, dateMatcher.start() - 80);
                int contextEnd = Math.min(text.length(), dateMatcher.end() + 80);
                String context = text.substring(contextStart, contextEnd);
                int dateCenter = ((dateMatcher.start() + dateMatcher.end()) / 2) - contextStart;
                StageRule closest = closestRule(context, dateCenter);
                if (closest == null) continue;
                String evidence = context.trim();
                boolean tentative = Pattern.compile("변경.{0,8}(가능|될 수)|일정.{0,8}변동|잠정")
                        .matcher(evidence).find();
                Found value = new Found(closest.type, closest.label, date, tentative, evidence, sourceUrl);
                result.putIfAbsent(closest.type + "|" + date, value);
            } catch (RuntimeException ignored) { }
        }
        return new ArrayList<>(result.values());
    }

    private StageRule closestRule(String context, int dateCenter) {
        StageRule closest = null;
        int closestDistance = Integer.MAX_VALUE;
        for (StageRule rule : RULES) {
            Matcher matcher = Pattern.compile(rule.words, Pattern.CASE_INSENSITIVE).matcher(context);
            while (matcher.find()) {
                int distance = Math.abs(((matcher.start() + matcher.end()) / 2) - dateCenter);
                if (distance < closestDistance) {
                    closest = rule;
                    closestDistance = distance;
                }
            }
        }
        return closestDistance <= 80 ? closest : null;
    }

    List<String> extractedSummary(String raw) {
        return extract(raw, "https://example.test/recruitment").stream()
                .map(value -> value.type + "|" + value.date + "|" + value.tentative)
                .toList();
    }
    void replace(JobPosting posting, List<Found> values) {
        Map<String, Found> unique = new LinkedHashMap<>();
        values.forEach(value -> unique.putIfAbsent(value.type + "|" + value.date, value));
        if (unique.isEmpty()) return;
        schedules.deleteByPostingId(posting.id);
        schedules.flush();
        for (Found found : unique.values()) {
            RecruitmentSchedule value = new RecruitmentSchedule();
            value.posting = posting; value.scheduleType = found.type; value.label = found.label;
            value.eventDate = found.date; value.tentative = found.tentative; value.evidence = found.evidence;
            value.sourceUrl = found.url; value.detectedAt = Instant.now(); schedules.save(value);
        }
    }

    private String pdfUrl(Document page, URI pageUri) {
        for (Element link : page.select("a[href]")) {
            String url = link.absUrl("href");
            String hint = (link.text() + " " + url).toLowerCase(Locale.ROOT);
            if (!hint.contains("pdf") || !(hint.contains("공고") || hint.contains("채용"))) continue;
            try {
                URI uri = URI.create(url);
                if ("https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
                        && (uri.getHost().equalsIgnoreCase(pageUri.getHost())
                        || uri.getHost().toLowerCase(Locale.ROOT).endsWith(".alio.go.kr"))) return url;
            } catch (RuntimeException ignored) { }
        }
        return null;
    }
}
