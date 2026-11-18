package kr.co.jobhub.auth.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.jobhub.auth.domain.AppUser;
import kr.co.jobhub.auth.repository.AppUserRepository;
import kr.co.jobhub.career.domain.JobAlertPreference;
import kr.co.jobhub.career.domain.UserProfile;
import kr.co.jobhub.career.repository.FavoriteOrganizationRepository;
import kr.co.jobhub.career.repository.JobAlertPreferenceRepository;
import kr.co.jobhub.career.repository.PushDeliveryReceiptRepository;
import kr.co.jobhub.career.repository.ScrapRepository;
import kr.co.jobhub.career.repository.UserProfileRepository;
import kr.co.jobhub.community.repository.InquiryRepository;
import kr.co.jobhub.success.repository.SuccessStoryRepository;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

/** 로그인한 사용자가 서비스에 저장한 자신의 데이터를 JSON으로 내려받게 한다. */
@RestController
public class AccountDataExportController {
    private final AppUserRepository users;
    private final UserProfileRepository profiles;
    private final ScrapRepository scraps;
    private final FavoriteOrganizationRepository favorites;
    private final JobAlertPreferenceRepository preferences;
    private final InquiryRepository inquiries;
    private final SuccessStoryRepository stories;
    private final PushDeliveryReceiptRepository deliveries;
    private final ObjectMapper json;

    public AccountDataExportController(AppUserRepository users, UserProfileRepository profiles,
                                       ScrapRepository scraps, FavoriteOrganizationRepository favorites,
                                       JobAlertPreferenceRepository preferences, InquiryRepository inquiries,
                                       SuccessStoryRepository stories, PushDeliveryReceiptRepository deliveries,
                                       ObjectMapper json) {
        this.users = users;
        this.profiles = profiles;
        this.scraps = scraps;
        this.favorites = favorites;
        this.preferences = preferences;
        this.inquiries = inquiries;
        this.stories = stories;
        this.deliveries = deliveries;
        this.json = json;
    }

    @GetMapping("/api/account/export")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> export(Principal principal) throws Exception {
        AppUser user = user(principal);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("exportedAt", java.time.Instant.now());
        result.put("account", Map.of(
                "email", user.email,
                "displayName", user.displayName,
                "preferredJobCategory", value(user.preferredJobCategory),
                "createdAt", user.createdAt,
                "termsAgreedAt", value(user.termsAgreedAt),
                "privacyAgreedAt", value(user.privacyAgreedAt),
                "consentVersion", value(user.consentVersion)));
        result.put("profile", profiles.findByUserId(user.id).map(this::profile).orElse(Map.of()));
        result.put("applications", scraps.findWithPostingByUserId(user.id).stream().map(item -> Map.ofEntries(
                Map.entry("postingId", item.posting.id),
                Map.entry("title", value(item.posting.title)),
                Map.entry("organization", value(item.posting.organization)),
                Map.entry("sourceUrl", value(item.posting.sourceUrl)),
                Map.entry("stage", value(item.stage)),
                Map.entry("applied", item.applied),
                Map.entry("scrappedAt", item.scrappedAt),
                Map.entry("appliedAt", value(item.appliedAt)),
                Map.entry("nextStepDate", value(item.nextStepDate)),
                Map.entry("memo", value(item.memo)),
                Map.entry("applicationProfile", structured(item.applicationProfile)))).toList());
        result.put("favoriteOrganizations", favorites.findByUserIdOrderByOrganizationName(user.id).stream()
                .map(item -> Map.of("organization", item.organizationName, "createdAt", item.createdAt)).toList());
        result.put("alertPreference", preferences.findByUserId(user.id).map(this::preference).orElse(Map.of()));
        result.put("inquiries", inquiries.findByUserIdOrderByCreatedAtDesc(user.id).stream().map(item -> Map.ofEntries(
                Map.entry("category", item.category), Map.entry("title", item.title),
                Map.entry("content", item.content), Map.entry("private", item.privateContent),
                Map.entry("status", item.status), Map.entry("answer", value(item.answer)),
                Map.entry("createdAt", item.createdAt), Map.entry("answeredAt", value(item.answeredAt)))).toList());
        result.put("successStories", stories.findByUserIdOrderByCreatedAtDesc(user.id).stream().map(item -> Map.of(
                "title", item.title, "content", item.content, "sharedProfile", structured(item.sharedProfile),
                "createdAt", item.createdAt, "updatedAt", item.updatedAt)).toList());
        result.put("pushDeliveryHistory", deliveries.findTop50ByUserIdOrderByDeliveredAtDesc(user.id).stream()
                .map(item -> Map.of("category", item.category, "deliveredAt", item.deliveredAt)).toList());

        byte[] body = json.writerWithDefaultPrettyPrinter().writeValueAsBytes(result);
        String filename = "public-job-hub-my-data-" + LocalDate.now(ZoneId.of("Asia/Seoul")) + ".json";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build());
        headers.setContentLength(body.length);
        return new ResponseEntity<>(body, headers, HttpStatus.OK);
    }

    private Map<String, Object> profile(UserProfile item) {
        return Map.ofEntries(Map.entry("birthDate", value(item.birthDate)),
                Map.entry("certifications", structured(item.certifications)),
                Map.entry("activities", structured(item.activities)),
                Map.entry("careerHistory", structured(item.careerHistory)),
                Map.entry("degree", structured(item.degree)), Map.entry("grades", structured(item.grades)),
                Map.entry("updatedAt", item.updatedAt));
    }

    private Map<String, Object> preference(JobAlertPreference item) {
        return Map.ofEntries(Map.entry("enabled", item.enabled), Map.entry("keywords", value(item.keywords)),
                Map.entry("regions", value(item.regions)), Map.entry("mobilityTypes", value(item.mobilityTypes)),
                Map.entry("newPostingAlerts", item.newPostingAlerts), Map.entry("deadlineAlerts", item.deadlineAlerts),
                Map.entry("postingChangeAlerts", Boolean.TRUE.equals(item.postingChangeAlerts)),
                Map.entry("importantChangeOnly", Boolean.TRUE.equals(item.importantChangeOnly)),
                Map.entry("deadlineDays", value(item.deadlineDays)), Map.entry("updatedAt", item.updatedAt));
    }

    private Object structured(String value) {
        if (value == null || value.isBlank()) return "";
        try {
            JsonNode node = json.readTree(value);
            return node == null ? "" : node;
        } catch (Exception ignored) {
            return value;
        }
    }

    private Object value(Object value) { return value == null ? "" : value; }

    private AppUser user(Principal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return users.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
