package kr.co.jobhub.qualification.repository;
import kr.co.jobhub.qualification.domain.QualificationCatalog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface QualificationCatalogRepository extends JpaRepository<QualificationCatalog,Long> {
 @Query("select q from QualificationCatalog q where q.active=true and lower(q.name) like lower(concat('%',:keyword,'%')) and (:type='' or q.type=:type) order by q.name,q.issuer")
 List<QualificationCatalog> search(@Param("keyword") String keyword,@Param("type") String type,Pageable pageable);
 Optional<QualificationCatalog> findByTypeAndNameAndIssuer(String type,String name,String issuer);
}
