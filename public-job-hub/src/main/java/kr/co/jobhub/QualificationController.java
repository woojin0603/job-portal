package kr.co.jobhub;
import kr.co.jobhub.model.QualificationCatalog;
import kr.co.jobhub.repo.QualificationCatalogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import java.util.*;
/** 자격증명 일부를 입력하면 공식 유형과 발급기관을 포함한 자동완성 결과를 제공한다. */
@RestController @RequestMapping("/api/qualifications")
public class QualificationController {
 public record QualificationView(Long id,String name,String type,String issuer,String officialCode){}
 private final QualificationCatalogRepository qualifications;
 public QualificationController(QualificationCatalogRepository qualifications){this.qualifications=qualifications;}
 @GetMapping public List<QualificationView> search(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="") String type){
  String keyword=q.trim(); if(keyword.length()<2)return List.of();
  return qualifications.search(keyword.substring(0,Math.min(100,keyword.length())),type,PageRequest.of(0,20)).stream().map(this::view).toList();
 }
 private QualificationView view(QualificationCatalog q){return new QualificationView(q.id,q.name,q.type,q.issuer,q.officialCode);}
}
