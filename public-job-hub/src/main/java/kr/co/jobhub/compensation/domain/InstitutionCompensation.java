package kr.co.jobhub.compensation.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** 알리오 기관코드와 기준연도별 신입사원 초임 공시값이다. 금액 단위는 천원이다. */
@Entity
@Table(name = "institution_compensations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"alio_institution_code", "fiscal_year"}),
        indexes = @Index(name = "idx_compensation_code_year", columnList = "alio_institution_code,fiscal_year"))
public class InstitutionCompensation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "alio_institution_code", nullable = false, length = 40)
    public String alioInstitutionCode;

    @Column(nullable = false, length = 200)
    public String organization;

    @Column(name = "fiscal_year", nullable = false)
    public int fiscalYear;

    public Long baseSalary;
    public Long fixedAllowance;
    public Long variableAllowance;
    public Long welfareBenefit;
    public Long performanceBonus;
    public Long managementEvaluationBonus;
    public Long otherAmount;

    @Column(nullable = false)
    public Long totalAmount;

    @Column(nullable = false, length = 30)
    public String valueType = "ACTUAL";

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt = Instant.now();
}
