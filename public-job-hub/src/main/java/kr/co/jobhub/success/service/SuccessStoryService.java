package kr.co.jobhub.success.service;

import kr.co.jobhub.auth.domain.AppUser;
import kr.co.jobhub.auth.repository.AppUserRepository;
import kr.co.jobhub.career.domain.Scrap;
import kr.co.jobhub.career.repository.ScrapRepository;
import kr.co.jobhub.success.domain.SuccessStory;
import kr.co.jobhub.success.repository.SuccessStoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.security.Principal;
import java.time.Instant;
import java.util.List;

@Service
public class SuccessStoryService {
    private final SuccessStoryRepository stories;
    private final ScrapRepository scraps;
    private final AppUserRepository users;

    public SuccessStoryService(SuccessStoryRepository stories, ScrapRepository scraps, AppUserRepository users) {
        this.stories = stories; this.scraps = scraps; this.users = users;
    }

    public List<SuccessStory> list() { return stories.findAllByOrderByCreatedAtDesc(); }

    public SuccessStory save(Long postingId, String title, String content, boolean shareProfile, Principal principal) {
        AppUser user = user(principal);
        Scrap scrap = scraps.findByUserIdAndPostingId(user.id, postingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "지원 기록을 찾을 수 없습니다."));
        if (!java.util.Set.of("DOCUMENT_PASSED", "INTERVIEW", "PASSED").contains(scrap.stage))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "서류 합격, 면접 예정 또는 최종 합격 공고만 공유할 수 있습니다.");
        SuccessStory value = stories.findByUserIdAndScrapId(user.id, scrap.id).orElseGet(SuccessStory::new);
        value.user = user; value.scrap = scrap; value.posting = scrap.posting;
        value.title = title.trim(); value.content = content.trim();
        value.sharedProfile = shareProfile && scrap.applicationProfile != null ? scrap.applicationProfile : null;
        value.updatedAt = Instant.now();
        return stories.save(value);
    }

    public void delete(Long id, Principal principal) {
        AppUser user = user(principal);
        SuccessStory value = stories.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!value.user.id.equals(user.id)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        stories.delete(value);
    }

    private AppUser user(Principal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return users.findByEmail(principal.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
