package kr.co.jobhub.qualification.controller;
import kr.co.jobhub.qualification.domain.QualificationCatalog;
import kr.co.jobhub.qualification.repository.QualificationCatalogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import java.util.*;
/** 자격증명 일부를 입력하면 공식 유형과 발급기관을 포함한 자동완성 결과를 제공한다. */
@RestController @RequestMapping("/api/qualifications")
public class QualificationController {
 public record QualificationView(Long id,String name,String type,String issuer,String officialCode,List<String> allowedGrades){}
 private final QualificationCatalogRepository qualifications;
 public QualificationController(QualificationCatalogRepository qualifications){this.qualifications=qualifications;}
 @GetMapping public List<QualificationView> search(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="") String type){
  String keyword=q.trim(); if(keyword.length()<2)return List.of();
  return qualifications.search(keyword.substring(0,Math.min(100,keyword.length())),type,PageRequest.of(0,20)).stream().map(this::view).toList();
 }
 private QualificationView view(QualificationCatalog q){return new QualificationView(q.id,q.name,q.type,q.issuer,q.officialCode,allowedGrades(q));}
 private List<String> allowedGrades(QualificationCatalog qualification){
  String name=qualification.name==null?"":qualification.name.trim();
  if(name.contains("한국사능력검정"))return List.of("심화 1급","심화 2급","심화 3급","기본 4급","기본 5급","기본 6급","직접입력");
  if(name.contains("컴퓨터활용능력"))return List.of("1급","2급","직접입력");
  if(name.equals("사회복지사")||name.equals("사회복지사 자격"))return List.of("1급","2급","직접입력");
  if(name.matches(".*사회복지사\\s*1급$"))return List.of("1급","직접입력");
  if(name.matches(".*사회복지사\\s*2급$"))return List.of("2급","직접입력");
  if(name.contains("워드프로세서")||name.equalsIgnoreCase("SQLD")||name.equalsIgnoreCase("SQLP")
    ||name.equalsIgnoreCase("ADsP")||name.equalsIgnoreCase("ADP"))
   return List.of("단일등급","직접입력");
  // "산업기사"도 "기사"로 끝나므로 더 긴 등급명을 먼저 검사해야 한다.
  for(String grade:List.of("기술사","기능장","산업기사","기사","기능사"))if(name.endsWith(grade))return List.of(grade,"직접입력");
  if("NATIONAL_TECHNICAL".equals(qualification.type))
   return List.of("단일등급","기술사","기능장","기사","산업기사","기능사","직접입력");
  return List.of("단일등급","직접입력");
 }
}
