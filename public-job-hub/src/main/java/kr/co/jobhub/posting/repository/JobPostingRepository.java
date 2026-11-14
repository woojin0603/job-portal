package kr.co.jobhub.posting.repository;

import kr.co.jobhub.posting.domain.JobPosting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.time.Instant;
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
    List<JobPosting> findByFirstSeenAtAfterOrderByFirstSeenAtDesc(Instant cutoff);

    /** 관리자 로고 관리 화면에 기관별 공고 수와 대표 공고 ID를 제공한다. */
    @Query("select p.organization, count(p.id), min(p.id) from JobPosting p group by p.organization")
    List<Object[]> organizationLogoCandidates();

    /** 관심기관 자동완성은 전체 공고를 메모리에 올리지 않고 DB에서 기관별로 집계한다. */
    @Query("select p.organization, count(p.id) from JobPosting p " +
           "where (:q = '' or lower(p.organization) like lower(concat('%', :q, '%'))) " +
           "group by p.organization order by count(p.id) desc, p.organization asc")
    List<Object[]> searchOrganizationCandidates(@Param("q") String q, Pageable pageable);

    boolean existsByOrganizationIgnoreCase(String organization);

    /** 공고 대표 지역과 직렬별 근무지역을 함께 반환한다. 컨트롤러에서 공고 ID 기준으로 중복 제거한다. */
    @Query("select p.id, p.region, rp.workRegion from JobPosting p " +
           "left join RecruitmentPosition rp on rp.posting = p " +
           "where (:includeClosed = true or p.deadline is null or p.deadline >= :today)")
    List<Object[]> visibleRegionSources(@Param("includeClosed") boolean includeClosed,
                                        @Param("today") LocalDate today);

    @Query("select count(p.id) from JobPosting p " +
           "where (:includeClosed = true or p.deadline is null or p.deadline >= :today)")
    long countVisible(@Param("includeClosed") boolean includeClosed, @Param("today") LocalDate today);

    /**
     * 공개 중인 공고를 제목·기관명으로 검색하고 20건씩 페이지로 나눈다.
     * mine이 참이면 지정 회원이 스크랩한 공고만 DB 쿼리 단계에서 필터링한다.
     */
    @Query("select p from JobPosting p where (:includeClosed = true or :mine = true or p.deadline is null or p.deadline >= :today) and " +
           "(lower(p.title) like lower(concat('%', :q, '%')) or lower(p.organization) like lower(concat('%', :q, '%'))) " +
           "and (:region = '' or lower(p.region) like lower(concat('%', :region, '%')) " +
           "or lower(p.region) like lower(concat('%', :regionFull, '%')) " +
           "or exists (select regionPosition.id from RecruitmentPosition regionPosition where regionPosition.posting = p " +
           "and (lower(regionPosition.workRegion) like lower(concat('%', :region, '%')) " +
           "or lower(regionPosition.workRegion) like lower(concat('%', :regionFull, '%'))))) " +
           "and (:district = '' or lower(p.region) like lower(concat('%', :district, '%')) " +
           "or exists (select districtPosition.id from RecruitmentPosition districtPosition where districtPosition.posting = p " +
           "and lower(districtPosition.workRegion) like lower(concat('%', :district, '%')))) " +
           "and (:mobility = '' or p.mobilityType = :mobility) " +
           "and (:institutionScope = '' or (p.organizationType = 'PUBLIC' and (" +
           "(:institutionScope = 'CENTRAL_PUBLIC' and (p.publicInstitutionType = 'CENTRAL_PUBLIC' " +
           "or p.alioInstitutionCode is not null or p.source in ('PUBLIC-DATA-RECRUITMENT', 'JOB-ALIO'))) " +
           "or (:institutionScope = 'LOCAL_PUBLIC' and p.publicInstitutionType = 'LOCAL_PUBLIC' " +
           "and p.alioInstitutionCode is null and p.source not in ('PUBLIC-DATA-RECRUITMENT', 'JOB-ALIO'))))) " +
           "and (:organizationForm = '' or lower(p.organization) like lower(concat('%', :organizationForm, '%'))) " +
           "and (:jobCategory = '' or exists (select rp.id from RecruitmentPosition rp where rp.posting = p " +
           "and (lower(rp.standardCategory) like lower(concat('%', :jobCategory, '%')) " +
           "or lower(rp.originalName) like lower(concat('%', :jobCategory, '%')) " +
           "or lower(rp.standardCategory) like lower(concat('%', :jobCategoryKeyword, '%')) " +
           "or lower(rp.originalName) like lower(concat('%', :jobCategoryKeyword, '%'))))) " +
           "and (:employmentType = '' or lower(p.employmentType) like lower(concat('%', :employmentType, '%')) " +
           "or lower(p.title) like lower(concat('%', :employmentType, '%')) " +
           "or lower(p.employmentType) like lower(concat('%', :employmentTypeKeyword, '%')) " +
           "or lower(p.title) like lower(concat('%', :employmentTypeKeyword, '%'))) " +
           "and (:employmentTypeExclude = '' or (lower(coalesce(p.employmentType, '')) not like lower(concat('%', :employmentTypeExclude, '%')) " +
           "and lower(p.title) not like lower(concat('%', :employmentTypeExclude, '%')))) " +
           "and (:employmentType <> '정규직' or (" +
           "lower(p.title) not like '%육아휴직%' and lower(p.title) not like '%대체%' " +
           "and lower(p.title) not like '%인턴%' and lower(p.title) not like '%기간제%' " +
           "and lower(p.title) not like '%무기%' and lower(p.title) not like '%공무직%' " +
           "and lower(p.title) not like '%계약직%' " +
           "and lower(coalesce(p.employmentType, '')) not like '%육아휴직%' " +
           "and lower(coalesce(p.employmentType, '')) not like '%대체%' " +
           "and lower(coalesce(p.employmentType, '')) not like '%인턴%' " +
           "and lower(coalesce(p.employmentType, '')) not like '%기간제%' " +
           "and lower(coalesce(p.employmentType, '')) not like '%무기%' " +
           "and lower(coalesce(p.employmentType, '')) not like '%공무직%' " +
           "and lower(coalesce(p.employmentType, '')) not like '%계약직%')) " +
           "and (:mine = false or exists (select s.id from Scrap s where s.posting = p and s.user.id = :userId)) " +
           "order by case when :preferredJobCategory <> '' and exists (select pref.id from RecruitmentPosition pref where pref.posting = p and lower(pref.standardCategory) like lower(concat('%', :preferredJobCategory, '%'))) then 0 else 1 end, " +
           "case when p.deadline >= :today then 0 when p.deadline is null then 1 else 2 end, " +
           "case when p.deadline >= :today then p.deadline else null end asc, " +
           "case when p.deadline < :today then p.deadline else null end desc, p.updatedAt desc")
    Page<JobPosting> search(@Param("q") String q, @Param("mine") boolean mine,
                            @Param("includeClosed") boolean includeClosed, @Param("today") LocalDate today,
                            @Param("region") String region, @Param("regionFull") String regionFull,
                            @Param("district") String district, @Param("mobility") String mobility,
                            @Param("institutionScope") String institutionScope,
                            @Param("organizationForm") String organizationForm,
                            @Param("jobCategory") String jobCategory,
                            @Param("jobCategoryKeyword") String jobCategoryKeyword,
                            @Param("employmentType") String employmentType,
                            @Param("employmentTypeKeyword") String employmentTypeKeyword,
                            @Param("employmentTypeExclude") String employmentTypeExclude,
                            @Param("preferredJobCategory") String preferredJobCategory,
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
