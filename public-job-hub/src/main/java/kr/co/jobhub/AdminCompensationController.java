package kr.co.jobhub;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import kr.co.jobhub.model.InstitutionCompensation;
import kr.co.jobhub.repo.InstitutionCompensationRepository;
import kr.co.jobhub.repo.JobPostingRepository;
import org.apache.poi.ss.usermodel.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.InputStream;
import java.time.Instant;
import java.util.*;

/** XLSX 파서나 관리 화면이 정규화한 알리오 초임 자료를 코드 기준으로 저장한다. */
@RestController
@RequestMapping("/api/admin/compensations")
public class AdminCompensationController {
    public record CompensationInput(@NotBlank @Size(max = 40) String alioInstitutionCode,
                                    @NotBlank @Size(max = 200) String organization,
                                    @Min(2000) @Max(2100) int fiscalYear,
                                    @PositiveOrZero Long baseSalary,
                                    @PositiveOrZero Long fixedAllowance,
                                    @PositiveOrZero Long variableAllowance,
                                    @PositiveOrZero Long welfareBenefit,
                                    @PositiveOrZero Long performanceBonus,
                                    @PositiveOrZero Long managementEvaluationBonus,
                                    @PositiveOrZero Long otherAmount,
                                    @NotNull @PositiveOrZero Long totalAmount,
                                    String valueType) {}

    public record ImportResult(int saved) {}
    public record UploadResult(int savedRows, int matchedInstitutions, int unmatchedInstitutions,
                               List<String> unmatchedNames) {}

    private final InstitutionCompensationRepository compensations;
    private final JobPostingRepository postings;

    public AdminCompensationController(InstitutionCompensationRepository compensations, JobPostingRepository postings) {
        this.compensations = compensations;
        this.postings = postings;
    }

    @PutMapping
    public ImportResult upsert(@RequestBody List<@Valid CompensationInput> inputs) {
        for (CompensationInput input : inputs) {
            String code = input.alioInstitutionCode().trim();
            InstitutionCompensation value = compensations
                    .findByAlioInstitutionCodeAndFiscalYear(code, input.fiscalYear())
                    .orElseGet(InstitutionCompensation::new);
            value.alioInstitutionCode = code;
            value.organization = input.organization().trim();
            value.fiscalYear = input.fiscalYear();
            value.baseSalary = input.baseSalary();
            value.fixedAllowance = input.fixedAllowance();
            value.variableAllowance = input.variableAllowance();
            value.welfareBenefit = input.welfareBenefit();
            value.performanceBonus = input.performanceBonus();
            value.managementEvaluationBonus = input.managementEvaluationBonus();
            value.otherAmount = input.otherAmount();
            value.totalAmount = input.totalAmount();
            value.valueType = "BUDGET".equalsIgnoreCase(input.valueType()) ? "BUDGET" : "ACTUAL";
            value.updatedAt = Instant.now();
            compensations.save(value);
        }
        return new ImportResult(inputs.size());
    }

    /** 알리오 직원평균보수 XLSX의 신입사원초임 시트를 읽어 기관코드를 붙여 저장한다. */
    @PostMapping("/upload")
    @Transactional
    public UploadResult upload(@RequestParam MultipartFile file) {
        if (file.isEmpty() || file.getSize() > 10_000_000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "10MB 이하 XLSX 파일을 선택해 주세요.");
        Map<String, Set<String>> codesByName = new HashMap<>();
        postings.findAll().stream().filter(p -> p.alioInstitutionCode != null && !p.alioInstitutionCode.isBlank())
                .forEach(p -> codesByName.computeIfAbsent(normalize(p.organization), key -> new LinkedHashSet<>())
                        .add(p.alioInstitutionCode));
        try (InputStream input = file.getInputStream(); Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = null;
            for (Sheet candidate : workbook) if (candidate.getSheetName().contains("신입사원초임")) sheet = candidate;
            if (sheet == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "신입사원초임 시트를 찾지 못했습니다.");
            DataFormatter formatter = new DataFormatter(Locale.KOREA);
            Row header = sheet.getRow(1);
            if (header == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "헤더 행을 찾지 못했습니다.");
            Map<Integer, Integer> years = new LinkedHashMap<>();
            for (Cell cell : header) {
                String label = formatter.formatCellValue(cell).trim();
                if (label.matches("20\\d{2}년")) years.put(cell.getColumnIndex(), Integer.parseInt(label.substring(0, 4)));
            }
            if (years.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "연도 열을 찾지 못했습니다.");
            Map<String, SalaryRows> grouped = new LinkedHashMap<>();
            for (int rowIndex = 2; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) continue;
                String organization = formatter.formatCellValue(row.getCell(0)).trim();
                String item = formatter.formatCellValue(row.getCell(3)).trim();
                if (organization.isBlank() || item.isBlank()) continue;
                for (var year : years.entrySet()) {
                    Long amount = amount(row.getCell(year.getKey()), formatter);
                    if (amount == null) continue;
                    grouped.computeIfAbsent(organization + "\u0000" + year.getValue(),
                            key -> new SalaryRows(organization, year.getValue())).put(item, amount);
                }
            }
            Set<String> matched = new HashSet<>();
            Set<String> unmatched = new TreeSet<>();
            int saved = 0;
            for (SalaryRows row : grouped.values()) {
                Set<String> codes = codesByName.getOrDefault(normalize(row.organization), Set.of());
                if (codes.size() != 1) { unmatched.add(row.organization); continue; }
                if (row.total == null) continue;
                String code = codes.iterator().next();
                InstitutionCompensation value = compensations.findByAlioInstitutionCodeAndFiscalYear(code, row.year)
                        .orElseGet(InstitutionCompensation::new);
                value.alioInstitutionCode = code;
                value.organization = row.organization;
                value.fiscalYear = row.year;
                value.baseSalary = row.baseSalary;
                value.fixedAllowance = row.fixedAllowance;
                value.variableAllowance = row.variableAllowance;
                value.welfareBenefit = row.welfareBenefit;
                value.performanceBonus = row.performanceBonus;
                value.managementEvaluationBonus = row.managementEvaluationBonus;
                value.otherAmount = row.otherAmount;
                value.totalAmount = row.total;
                value.valueType = row.year == years.values().stream().mapToInt(Integer::intValue).max().orElse(row.year)
                        ? "BUDGET" : "ACTUAL";
                value.updatedAt = Instant.now();
                compensations.save(value);
                matched.add(row.organization);
                saved++;
            }
            return new UploadResult(saved, matched.size(), unmatched.size(), unmatched.stream().limit(30).toList());
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "XLSX 파일을 읽지 못했습니다.");
        }
    }

    private Long amount(Cell cell, DataFormatter formatter) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;
        if (cell.getCellType() == CellType.NUMERIC) return Math.round(cell.getNumericCellValue());
        String value = formatter.formatCellValue(cell).replace(",", "").trim();
        try { return value.isBlank() ? null : Math.round(Double.parseDouble(value)); }
        catch (NumberFormatException ignored) { return null; }
    }

    private String normalize(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.KOREA)
                .replaceAll("^(\\(주\\)|㈜|\\(재\\)|\\(사\\)|주식회사|재단법인|사단법인)", "")
                .replaceAll("[^가-힣a-z0-9]", "");
    }

    private static class SalaryRows {
        final String organization;
        final int year;
        Long baseSalary, fixedAllowance, variableAllowance, welfareBenefit, performanceBonus;
        Long managementEvaluationBonus, otherAmount, total;

        SalaryRows(String organization, int year) { this.organization = organization; this.year = year; }

        void put(String item, Long amount) {
            switch (item.replace(" ", "")) {
                case "기본급" -> baseSalary = amount;
                case "고정수당" -> fixedAllowance = amount;
                case "실적수당" -> variableAllowance = amount;
                case "급여성복리후생비" -> welfareBenefit = amount;
                case "성과상여금" -> performanceBonus = amount;
                case "(경영평가성과급)" -> managementEvaluationBonus = amount;
                case "기타" -> otherAmount = amount;
                case "합계" -> total = amount;
            }
        }
    }
}
