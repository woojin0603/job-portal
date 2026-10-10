package kr.co.jobhub.posting.repository;

import kr.co.jobhub.posting.domain.JobPosting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** 채용공고 검색과 수집 시 중복 확인을 담당하는 JPA 저장소. */
public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {
    /** 수집 출처와 원본 ID 조합으로 이미 저장한 공고를 찾는다. */
    Optional<JobPosting> findBySourceAndSourceId(String source, String sourceId);
    Optional<JobPosting> findFirstBySourceUrl(String sourceUrl);
    Optional<JobPosting> findFirstByOrganizationAndTitleAndPostedAtAndDeadline(
            String organization, String title, LocalDate postedAt, LocalDate deadline);
    List<JobPosting> findByAlioInstitutionCodeOrderByPostedAtDesc(String alioInstitutionCode);

    /**
     * 공개 중인 공고를 제목·기관명으로 검색하고 20건씩 페이지로 나눈다.
     * mine이 참이면 지정 회원이 스크랩한 공고만 DB 쿼리 단계에서 필터링한다.
     */
    @Query("select p from JobPosting p where (:includeClosed = true or :mine = true or p.deadline is null or p.deadline >= :today) and " +
           "(lower(p.title) like lower(concat('%', :q, '%')) or lower(p.organization) like lower(concat('%', :q, '%'))) " +
           "and (:region = '' or lower(p.region) like lower(concat('%', :region, '%')) " +
           "or lower(p.region) like lower(concat('%', :regionFull, '%'))) " +
           "and (:district = '' or lower(p.region) like lower(concat('%', :district, '%'))) " +
           "and (:mobility = '' or p.mobilityType = :mobility) " +
           "and (:jobCategory = '' or exists (select rp.id from RecruitmentPosition rp where rp.posting = p " +
           "and lower(rp.standardCategory) like lower(concat('%', :jobCategory, '%')))) " +
           "and (:mine = false or exists (select s.id from Scrap s where s.posting = p and s.user.id = :userId)) " +
           "order by case when p.deadline >= :today then 0 when p.deadline is null then 1 else 2 end, " +
           "case when p.deadline >= :today then p.deadline else null end asc, " +
           "case when p.deadline < :today then p.deadline else null end desc, p.updatedAt desc")
    Page<JobPosting> search(@Param("q") String q, @Param("mine") boolean mine,
                            @Param("includeClosed") boolean includeClosed, @Param("today") LocalDate today,
                            @Param("region") String region, @Param("regionFull") String regionFull,
                            @Param("district") String district, @Param("mobility") String mobility,
                            @Param("jobCategory") String jobCategory,
                            @Param("userId") Long userId, Pageable pageable);

    /** 마감된 공고를 최신 마감일순으로 검색하며 원본 데이터는 삭제하지 않는다. */
    @Query("select p from JobPosting p where p.deadline is not null and p.deadline < :today and " +
           "(lower(p.title) like lower(concat('%', :q, '%')) or lower(p.organization) like lower(concat('%', :q, '%'))) " +
           "and (:region = '' or lower(p.region) like lower(concat('%', :region, '%'))) " +
           "and (:employmentType = '' or lower(p.employmentType) like lower(concat('%', :employmentType, '%'))) " +
           "and (:fromDate is null or coalesce(p.postedAt, p.deadline) >= :fromDate) " +
           "and (:toDate is null or coalesce(p.postedAt, p.deadline) <= :toDate) " +
           "and (:jobCategory = '' or exists (select rp.id from RecruitmentPosition rp where rp.posting = p " +
           "and lower(rp.standardCategory) like lower(concat('%', :jobCategory, '%')))) " +
           "order by p.deadline desc, p.updatedAt desc")
    Page<JobPosting> searchArchive(@Param("q") String q, @Param("today") LocalDate today,
                                   @Param("region") String region,
                                   @Param("employmentType") String employmentType,
                                   @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate,
                                   @Param("jobCategory") String jobCategory, Pageable pageable);
}
