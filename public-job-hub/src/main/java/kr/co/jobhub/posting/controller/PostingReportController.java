package kr.co.jobhub.posting.controller;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import kr.co.jobhub.auth.domain.AppUser;
import kr.co.jobhub.auth.repository.AppUserRepository;
import kr.co.jobhub.posting.domain.*;
import kr.co.jobhub.posting.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
@RestController @RequestMapping("/api")
public class PostingReportController {
 public record CreateRequest(@NotBlank @Size(max=40) String category,@NotBlank @Size(max=2000) String content) {}
 public record StatusRequest(@Pattern(regexp="RECEIVED|CHECKING|RESOLVED") String status,@Size(max=2000) String adminNote) {}
 public record Item(Long id,Long postingId,String title,String organization,String category,String content,String status,String adminNote,Instant createdAt,Instant resolvedAt,String reporter) {}
 private final PostingReportRepository reports; private final JobPostingRepository postings; private final AppUserRepository users;
 public PostingReportController(PostingReportRepository r,JobPostingRepository p,AppUserRepository u){reports=r;postings=p;users=u;}
 @PostMapping("/postings/{id}/reports") public Item create(@PathVariable Long id,@Valid @RequestBody CreateRequest request,Principal principal){
  PostingReport value=new PostingReport(); value.posting=postings.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  value.user=user(principal); value.category=request.category().trim(); value.content=request.content().trim(); return item(reports.save(value)); }
 @GetMapping("/admin/posting-reports") public List<Item> list(){return reports.findAllByOrderByCreatedAtDesc().stream().map(this::item).toList();}
 @PostMapping("/admin/posting-reports/{id}") public Item update(@PathVariable Long id,@Valid @RequestBody StatusRequest request){
  PostingReport value=reports.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND)); value.status=request.status(); value.adminNote=request.adminNote();
  value.resolvedAt="RESOLVED".equals(value.status)?Instant.now():null; return item(reports.save(value)); }
 private AppUser user(Principal p){if(p==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);return users.findByEmail(p.getName()).orElseThrow();}
 private Item item(PostingReport v){return new Item(v.id,v.posting.id,v.posting.title,v.posting.organization,v.category,v.content,v.status,v.adminNote,v.createdAt,v.resolvedAt,v.user.email);}
}
