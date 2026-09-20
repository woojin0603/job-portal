package kr.co.jobhub;

import kr.co.jobhub.model.*;
import kr.co.jobhub.repo.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.net.URI;
import java.security.Principal;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * React 화면에 채용공고 목록·개인 스크랩·원문 미리보기 데이터를 제공하는 API다.
 * 엔티티를 그대로 노출하지 않고 화면별 응답 객체로 변환한다.
 */
@RestController
@RequestMapping("/api")
public class ApiController {
    /** 카드 한 장에 필요한 공고 정보와 현재 회원의 개인 상태. */
    public record PostingDto(Long id, String source, String title, String organization, String region,
                             String employmentType, LocalDate postedAt, LocalDate deadline,
                             String sourceUrl, boolean scrapped, boolean applied) {
    }

    /** 현재 페이지 목록과 다음 페이지 여부 및 최근 수집 상태. */
    public record PostingPage(List<PostingDto> items, long total, int page, boolean hasNext,
                              CrawlService.Status crawlStatus) {
    }

    /** 스크랩·지원 토글 후 React가 즉시 사용할 상태. */
    public record ScrapState(boolean scrapped, boolean applied) {
    }

    /** 외부 상세 페이지의 구조와 이미지를 유지하는 격리 프레임용 HTML. */
    public record Preview(String title, String organization, String sourceUrl, String html) {
    }

    private final JobPostingRepository postings;
    private final ScrapRepository scraps;
    private final AppUserRepository users;
    private final CrawlService crawler;

    /** 공고·회원·스크랩 저장소와 수집 상태 제공자를 주입받는다. */
    public ApiController(JobPostingRepository postings, ScrapRepository scraps,
                         AppUserRepository users, CrawlService crawler) {
        this.postings = postings;
        this.scraps = scraps;
        this.users = users;
        this.crawler = crawler;
    }

    /** 인증 주체의 이메일로 회원을 확인하며 익명 요청은 거부한다. */
    private AppUser user(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return users.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    /** 운영자가 예약 수집의 최근 성공·실패 시각을 확인할 수 있게 한다. */
    @GetMapping("/crawl-status")
    public CrawlService.Status crawlStatus() {
        return crawler.status();
    }

    /** 검색어·마이페이지 여부·페이지 번호에 맞는 공고 20건을 반환한다. */
    @GetMapping("/postings")
    public PostingPage list(@RequestParam(defaultValue = "") String q,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "false") boolean mine, Principal principal) {
        long userId = principal == null ? 0L : user(principal).id;
        int safePage = Math.max(0, page);
        var result = postings.search(q, mine, userId,
                PageRequest.of(safePage, 20, Sort.by(Sort.Direction.DESC, "updatedAt")));
        Map<Long, Scrap> saved = scraps.findByUserId(userId).stream()
                .collect(Collectors.toMap(s -> s.posting.getId(), Function.identity()));
        List<PostingDto> items = result.getContent().stream()
                .map(p -> {
                    Scrap s = saved.get(p.id);
                    return new PostingDto(p.id, p.source, p.title, p.organization, p.region,
                            p.employmentType, p.postedAt, p.deadline, p.sourceUrl, s != null, s != null && s.applied);
                })
                .toList();
        return new PostingPage(items, result.getTotalElements(), safePage, result.hasNext(), crawler.status());
    }

    /** 공고 스크랩을 토글한다. 해제하면 개인 지원 완료 상태도 함께 제거된다. */
    @PostMapping("/postings/{id}/scrap")
    public ScrapState toggleScrap(@PathVariable Long id, Principal principal) {
        AppUser user = user(principal);
        JobPosting posting = postings.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var existing = scraps.findByUserIdAndPostingId(user.id, id);
        if (existing.isPresent()) {
            scraps.delete(existing.get());
            return new ScrapState(false, false);
        }
        Scrap s = new Scrap();
        s.user = user;
        s.posting = posting;
        scraps.save(s);
        return new ScrapState(true, false);
    }

    /** 스크랩한 공고의 지원 완료 여부와 적용 시각을 토글한다. */
    @PostMapping("/postings/{id}/applied")
    public ScrapState toggleApplied(@PathVariable Long id, Principal principal) {
        Scrap s = scraps.findByUserIdAndPostingId(user(principal).id, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Scrap first"));
        s.applied = !s.applied;
        s.appliedAt = s.applied ? Instant.now() : null;
        scraps.save(s);
        return new ScrapState(true, s.applied);
    }

    /**
     * 등록된 잡알리오 상세 페이지의 HTML 구조, 표, 이미지 및 스타일시트를 보존한다.
     * 신뢰할 수 있는 출처만 읽고 활성 콘텐츠를 제거하여 sandbox iframe에 제공한다.
     */
    @GetMapping("/postings/{id}/preview")
    public Preview preview(@PathVariable Long id) {
        JobPosting p = postings.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        URI uri = URI.create(p.sourceUrl);
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || !"job.alio.go.kr".equalsIgnoreCase(uri.getHost())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "미리보기를 제공할 수 없습니다.");
        }
        try {
            Document doc = Jsoup.connect(p.sourceUrl).userAgent("Mozilla/5.0 PublicJobHub/0.3")
                    .followRedirects(false).maxBodySize(3_000_000).timeout(12000).get();
            Element original = doc.selectFirst("#contentRV, main, article");
            if (original == null) {
                original = doc.body();
            }
            Element content = original.clone();
            content.select("script, iframe, object, embed, form, meta, base").remove();
            for (Element element : content.getAllElements()) {
                for (var attribute : List.copyOf(element.attributes().asList())) {
                    String key = attribute.getKey().toLowerCase();
                    if (key.startsWith("on")) {
                        element.removeAttr(attribute.getKey());
                    } else if (List.of("href", "src", "poster", "data-src").contains(key)) {
                        String absolute = element.absUrl(attribute.getKey());
                        if (absolute.startsWith("https://") || absolute.startsWith("http://")) {
                            element.attr(attribute.getKey(), absolute);
                        } else {
                            element.removeAttr(attribute.getKey());
                        }
                    } else if (key.equals("srcset")) {
                        element.removeAttr(attribute.getKey());
                    }
                }
                if (element.hasAttr("data-src") && !element.hasAttr("src")) {
                    element.attr("src", element.attr("data-src"));
                }
                if (element.normalName().equals("a")) {
                    element.attr("target", "_blank").attr("rel", "noopener noreferrer");
                }
            }
            StringBuilder styles = new StringBuilder();
            for (Element link : doc.select("head link[rel=stylesheet]")) {
                String href = link.absUrl("href");
                if (href.startsWith("https://") || href.startsWith("http://")) {
                    styles.append("<link rel=\"stylesheet\" href=\"")
                            .append(org.jsoup.nodes.Entities.escape(href)).append("\">");
                }
            }
            String html = "<!doctype html><html lang=\"ko\"><head><meta charset=\"utf-8\">"
                    + "<meta http-equiv=\"Content-Security-Policy\" content=\"default-src 'none'; "
                    + "img-src https: http: data:; style-src https: http: 'unsafe-inline'; "
                    + "font-src https: http: data:;\">"
                    + styles
                    + "<style>html,body{margin:0;padding:0;background:white;}"
                    + "body{overflow-x:auto;}#contentRV,#contentRV #txt{width:auto!important;min-width:0!important;"
                    + "max-width:none!important;margin:0!important;padding:12px!important;}"
                    + "img{max-width:100%;height:auto;}table{max-width:100%;}</style></head><body>"
                    + content.outerHtml() + "</body></html>";
            return new Preview(p.title, p.organization, p.sourceUrl, html);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "원문을 가져오지 못했습니다.");
        }
    }
}
