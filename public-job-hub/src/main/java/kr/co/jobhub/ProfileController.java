package kr.co.jobhub;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import kr.co.jobhub.model.AppUser;
import kr.co.jobhub.model.UserProfile;
import kr.co.jobhub.repo.AppUserRepository;
import kr.co.jobhub.repo.UserProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** 회원 본인만 구조화된 채용 스펙을 읽고 저장하며 민감한 식별정보 입력을 차단한다. */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private static final java.util.regex.Pattern RESIDENT_ID =
            java.util.regex.Pattern.compile("(?<!\\d)\\d{6}-?[1-8]\\d{6}(?!\\d)");
    private static final java.util.regex.Pattern PHONE = java.util.regex.Pattern.compile(
            "(?<!\\d)(?:01[016789][ -]?\\d{3,4}[ -]?\\d{4}|0\\d{1,2}[ -]?\\d{3,4}[ -]?\\d{4})(?!\\d)");
    private static final java.util.regex.Pattern EMAIL =
            java.util.regex.Pattern.compile("[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}");
    private static final java.util.regex.Pattern FORBIDDEN_LABEL = java.util.regex.Pattern.compile(
            "(?i)(주민(?:등록)?번호|전화번호|휴대폰|연락처|자격증\\s*(?:번호|등록번호)|학위\\s*번호|여권번호|운전면허번호|계좌번호|상세주소)");
    private static final String MONTH = "^$|^\\d{4}-(0[1-9]|1[0-2])$";

    public record Certification(@Size(max = 120) String name,
                                @Pattern(regexp = MONTH) String acquiredMonth) {}
    public record Activity(@Size(max = 160) String name, @Size(max = 1500) String description,
                           @Pattern(regexp = MONTH) String startMonth,
                           @Pattern(regexp = MONTH) String endMonth) {}
    public record Career(@Size(max = 160) String companyName, @Size(max = 120) String position,
                         @Size(max = 2000) String duties,
                         @Pattern(regexp = MONTH) String startMonth,
                         @Pattern(regexp = MONTH) String endMonth) {}
    public record Degree(@Size(max = 160) String schoolName, @Size(max = 160) String major,
                         @Size(max = 100) String degreeType,
                         @Pattern(regexp = MONTH) String startMonth,
                         @Pattern(regexp = MONTH) String endMonth,
                         @Size(max = 80) String status) {}

    public record ProfileRequest(@Past LocalDate birthDate,
                                 @Valid @Size(max = 30) List<Certification> certifications,
                                 @Valid @Size(max = 30) List<Activity> activities,
                                 @Valid @Size(max = 30) List<Career> careers,
                                 @Valid @Size(max = 20) List<Degree> degrees,
                                 @Size(max = 1000) String grades) {}
    public record ProfileResponse(String displayName, LocalDate birthDate,
                                  List<Certification> certifications, List<Activity> activities,
                                  List<Career> careers, List<Degree> degrees,
                                  String grades, Instant updatedAt) {}

    private final UserProfileRepository profiles;
    private final AppUserRepository users;
    private final ObjectMapper json;

    public ProfileController(UserProfileRepository profiles, AppUserRepository users, ObjectMapper json) {
        this.profiles = profiles;
        this.users = users;
        this.json = json;
    }

    @GetMapping
    public ProfileResponse get(Principal principal) {
        AppUser user = user(principal);
        return profiles.findByUserId(user.id).map(profile -> response(user, profile))
                .orElseGet(() -> new ProfileResponse(user.displayName, null, List.of(), List.of(),
                        List.of(), List.of(), "", null));
    }

    @PostMapping
    public ProfileResponse save(@Valid @RequestBody ProfileRequest request, Principal principal) {
        AppUser user = user(principal);
        List<Certification> certifications = request.certifications() == null ? List.of() : request.certifications();
        List<Activity> activities = request.activities() == null ? List.of() : request.activities();
        List<Career> careers = request.careers() == null ? List.of() : request.careers();
        List<Degree> degrees = request.degrees() == null ? List.of() : request.degrees();
        List<String> values = new ArrayList<>();
        certifications.forEach(item -> values.add(item.name()));
        activities.forEach(item -> { values.add(item.name()); values.add(item.description()); });
        careers.forEach(item -> { values.add(item.companyName()); values.add(item.position()); values.add(item.duties()); });
        degrees.forEach(item -> { values.add(item.schoolName()); values.add(item.major()); values.add(item.degreeType()); });
        values.add(request.grades());
        rejectSensitive(values);

        UserProfile profile = profiles.findByUserId(user.id).orElseGet(UserProfile::new);
        profile.user = user;
        profile.birthDate = request.birthDate();
        profile.certifications = write(certifications);
        profile.activities = write(activities);
        profile.careerHistory = write(careers);
        profile.degree = write(degrees);
        profile.grades = clean(request.grades());
        profile.updatedAt = Instant.now();
        return response(user, profiles.save(profile));
    }

    private AppUser user(Principal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return users.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private void rejectSensitive(List<String> values) {
        for (String value : values) {
            String text = clean(value);
            if (RESIDENT_ID.matcher(text).find() || PHONE.matcher(text).find()
                    || EMAIL.matcher(text).find() || FORBIDDEN_LABEL.matcher(text).find()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "전화번호·이메일·주민번호·자격증 번호·학위 번호 등 개인정보는 저장할 수 없습니다.");
            }
        }
    }

    private String write(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private ProfileResponse response(AppUser user, UserProfile profile) {
        return new ProfileResponse(user.displayName, profile.birthDate,
                readCertifications(profile.certifications), readActivities(profile.activities),
                readCareers(profile.careerHistory), readDegrees(profile.degree),
                clean(profile.grades), profile.updatedAt);
    }

    private List<Certification> readCertifications(String raw) {
        try { return json.readValue(raw, new TypeReference<>() {}); }
        catch (Exception e) { return clean(raw).isBlank() ? List.of() : List.of(new Certification(clean(raw), "")); }
    }
    private List<Activity> readActivities(String raw) {
        try { return json.readValue(raw, new TypeReference<>() {}); }
        catch (Exception e) { return clean(raw).isBlank() ? List.of() : List.of(new Activity("", clean(raw), "", "")); }
    }
    private List<Career> readCareers(String raw) {
        try { return json.readValue(raw, new TypeReference<>() {}); }
        catch (Exception e) { return clean(raw).isBlank() ? List.of() : List.of(new Career("", "", clean(raw), "", "")); }
    }
    private List<Degree> readDegrees(String raw) {
        try { return json.readValue(raw, new TypeReference<>() {}); }
        catch (Exception e) { return clean(raw).isBlank() ? List.of() : List.of(new Degree("", clean(raw), "", "", "", "")); }
    }
    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
