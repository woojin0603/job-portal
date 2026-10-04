package kr.co.jobhub.model;
import jakarta.persistence.*;
import java.time.Instant;
/** 공식 출처에서 가져온 국내 자격 종목 검색 기준정보다. */
@Entity @Table(name="qualification_catalog", indexes={@Index(name="idx_qualification_name",columnList="name"),@Index(name="idx_qualification_type",columnList="type")})
public class QualificationCatalog {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false,length=200) public String name;
 @Column(nullable=false,length=30) public String type;
 @Column(nullable=false,length=200) public String issuer="";
 @Column(name="official_code",length=100) public String officialCode;
 @Column(nullable=false,length=80) public String source;
 @Column(nullable=false) public boolean active=true;
 @Column(name="source_updated_at",nullable=false) public Instant sourceUpdatedAt=Instant.now();
}
