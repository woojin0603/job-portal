package kr.co.jobhub;
import kr.co.jobhub.model.QualificationCatalog;
import kr.co.jobhub.repo.QualificationCatalogRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.*; import java.nio.ByteBuffer; import java.nio.charset.*; import java.time.Instant; import java.util.*;
/** 관리자가 공식 CSV 파일을 유형별로 적재해 외부 사이트 장애와 무관하게 검색하게 한다. */
@RestController @RequestMapping("/api/admin/qualifications")
public class AdminQualificationController {
 public record ImportResult(int imported,int skipped){}
 private final QualificationCatalogRepository qualifications;
 public AdminQualificationController(QualificationCatalogRepository qualifications){this.qualifications=qualifications;}
 @PostMapping(value="/import",consumes="multipart/form-data")
 public ImportResult importCsv(@RequestParam MultipartFile file,@RequestParam String type,@RequestParam(defaultValue="AUTO") String encoding){
  if(!List.of("AUTO","NATIONAL_TECHNICAL","NATIONAL_PROFESSIONAL","ACCREDITED_PRIVATE").contains(type))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"자격 유형이 올바르지 않습니다.");
  int imported=0,skipped=0;
  try{byte[] bytes=file.getBytes(); Charset charset=charset(bytes,encoding); BufferedReader reader=new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes),charset));
   List<String> headers=parse(reader.readLine()); String line;
   while((line=reader.readLine())!=null){List<String> values=parse(line); String name=value(headers,values,"자격명","자격종목명","종목명","국가자격명칭"); String issuer=value(headers,values,"시행기관","발급기관","자격발급기관","기관명"); String code=value(headers,values,"종목코드","자격코드","등록번호"); String rowType=type.equals("AUTO")?detectedType(headers,values):type; if(name.isBlank()||rowType.isBlank()){skipped++;continue;} String cleanIssuer=issuer.trim(); QualificationCatalog item=qualifications.findByTypeAndNameAndIssuer(rowType,name.trim(),cleanIssuer).orElseGet(QualificationCatalog::new); item.name=name.trim();item.type=rowType;item.issuer=cleanIssuer;item.officialCode=code.isBlank()?null:code.trim();item.source="한국산업인력공단_국가자격 종목 목록 정보";item.active=true;item.sourceUpdatedAt=Instant.now();qualifications.save(item);imported++;}
   reader.close();
   return new ImportResult(imported,skipped);
  }catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"CSV를 읽지 못했습니다. 인코딩과 열 이름을 확인해 주세요.");}
 }
 private String detectedType(List<String> h,List<String> v){String code=value(h,v,"자격구분코드");String name=value(h,v,"자격구분명");if("T".equalsIgnoreCase(code)||name.contains("기술"))return "NATIONAL_TECHNICAL";if("S".equalsIgnoreCase(code)||name.contains("전문"))return "NATIONAL_PROFESSIONAL";return "";}
 private Charset charset(byte[] bytes,String requested)throws CharacterCodingException{if(!"AUTO".equalsIgnoreCase(requested))return Charset.forName(requested);CharsetDecoder decoder=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);try{decoder.decode(ByteBuffer.wrap(bytes));return StandardCharsets.UTF_8;}catch(CharacterCodingException ignored){return Charset.forName("MS949");}}
 private String value(List<String> h,List<String> v,String... names){for(String name:names)for(int i=0;i<h.size();i++)if(h.get(i).replace("\uFEFF","").trim().equals(name)&&i<v.size())return v.get(i);return "";}
 private List<String> parse(String line){if(line==null)return List.of();List<String> out=new ArrayList<>();StringBuilder value=new StringBuilder();boolean quoted=false;for(int i=0;i<line.length();i++){char c=line.charAt(i);if(c=='"'&&quoted&&i+1<line.length()&&line.charAt(i+1)=='"'){value.append('"');i++;}else if(c=='"')quoted=!quoted;else if(c==','&&!quoted){out.add(value.toString());value.setLength(0);}else value.append(c);}out.add(value.toString());return out;}
}
