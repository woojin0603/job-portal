package kr.co.jobhub.posting.controller;

import kr.co.jobhub.posting.domain.InstitutionLogo;
import kr.co.jobhub.posting.repository.InstitutionLogoRepository;
import kr.co.jobhub.posting.repository.JobPostingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/** 수집된 기관별 자동 로고 상태와 운영자 지정 로고를 관리한다. */
@RestController
@RequestMapping("/api/admin/institution-logos")
public class AdminInstitutionLogoController {
    public record LogoRequest(String organization, String logoUrl) {}
    public record LogoItem(String organization, long postingCount, Long representativePostingId,
                           String logoUrl, boolean manual) {}

    private final InstitutionLogoRepository logos;
    private final JobPostingRepository postings;

    public AdminInstitutionLogoController(InstitutionLogoRepository logos, JobPostingRepository postings) {
        this.logos = logos;
        this.postings = postings;
    }

    @GetMapping
    public List<LogoItem> list() {
        return postings.organizationLogoCandidates().stream().map(row -> {
            String organization = (String) row[0];
            InstitutionLogo saved = logos.findByOrganizationIgnoreCase(organization).orElse(null);
            return new LogoItem(organization, ((Number) row[1]).longValue(), ((Number) row[2]).longValue(),
                    saved == null ? null : saved.logoUrl, saved != null);
        }).sorted(Comparator.comparing(LogoItem::manual).thenComparing(LogoItem::organization)).toList();
    }

    @PostMapping
    public LogoItem save(@RequestBody LogoRequest request) {
        String organization = request.organization() == null ? "" : request.organization().trim();
        String logoUrl = request.logoUrl() == null ? "" : request.logoUrl().trim();
        if (organization.isBlank() || organization.length() > 200 || !validLogoUrl(logoUrl)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "기관명과 HTTPS 로고 주소를 확인해 주세요.");
        }
        InstitutionLogo logo = logos.findByOrganizationIgnoreCase(organization).orElseGet(InstitutionLogo::new);
        logo.organization = organization;
        logo.logoUrl = logoUrl;
        logo.updatedAt = Instant.now();
        logos.save(logo);
        Object[] candidate = postings.organizationLogoCandidates().stream()
                .filter(row -> organization.equalsIgnoreCase((String) row[0])).findFirst().orElse(null);
        return new LogoItem(organization, candidate == null ? 0 : ((Number) candidate[1]).longValue(),
                candidate == null ? null : ((Number) candidate[2]).longValue(), logoUrl, true);
    }

    @PostMapping("/delete")
    public void delete(@RequestBody LogoRequest request) {
        if (request.organization() == null) return;
        logos.findByOrganizationIgnoreCase(request.organization().trim()).ifPresent(logos::delete);
    }

    private boolean validLogoUrl(String value) {
        try {
            URI uri = URI.create(value);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
