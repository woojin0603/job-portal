package kr.co.jobhub.admin.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 호스트 점검 스크립트가 발행한 읽기 전용 상태만 관리자 화면에 전달한다. */
@Service
public class OperationsStatusService {
    public record JobStatus(String key, String label, String state, Instant updatedAt,
                            Integer exitCode, boolean stale, String summary) {}
    public record ServerMetrics(Boolean appHealthy, Boolean caddyRunning, Boolean httpsReachable,
                                Boolean databaseReachable, Integer availableMemoryMb,
                                Integer diskUsagePercent, Integer latestBackupAgeHours) {}
    public record OperationsStatus(List<JobStatus> jobs, ServerMetrics server, Instant checkedAt) {}

    private static final Pattern MEMORY = Pattern.compile("Available memory: (\\d+) MB");
    private static final Pattern DISK = Pattern.compile("Disk usage: (\\d+)%");
    private static final Pattern BACKUP_AGE = Pattern.compile("Latest backup age: (\\d+) hour");
    private static final Pattern BACKUP_SIZE = Pattern.compile("TiDB backup completed: .* \\(([^)]+)\\)");
    private static final Pattern OFFSITE_FILE = Pattern.compile("Encrypted off-site backup uploaded: ([^ ]+) \\(([^)]+)\\)");

    private final Path statusDirectory;

    public OperationsStatusService(@Value("${jobhub.operations.status-directory:./backups/monitor-state}") String directory) {
        this.statusDirectory = Path.of(directory).toAbsolutePath().normalize();
    }

    public OperationsStatus status() {
        ParsedJob health = read("health", "서버 상태", Duration.ofHours(2));
        ParsedJob backup = read("backup", "TiDB 백업", Duration.ofHours(30));
        ParsedJob offsite = read("offsite-backup", "외부 암호화 백업", Duration.ofDays(8));
        ParsedJob storage = read("storage", "저장공간 정리", Duration.ofDays(8));
        return new OperationsStatus(
                List.of(health.view(), backup.view(), offsite.view(), storage.view()),
                metrics(health.output()), Instant.now());
    }

    private ParsedJob read(String key, String label, Duration staleAfter) {
        Path file = statusDirectory.resolve(key + ".properties").normalize();
        if (!file.startsWith(statusDirectory) || !Files.isRegularFile(file)) {
            return new ParsedJob(new JobStatus(key, label, "unknown", null, null, true, "실행 기록 없음"), "");
        }
        try (InputStream input = Files.newInputStream(file)) {
            Properties properties = new Properties();
            properties.load(input);
            Instant updatedAt = parseInstant(properties.getProperty("updatedAt"));
            Integer exitCode = parseInteger(properties.getProperty("exitCode"));
            String state = "healthy".equals(properties.getProperty("state")) ? "healthy" : "failed";
            String output = decode(properties.getProperty("outputBase64"));
            boolean stale = updatedAt == null || updatedAt.plus(staleAfter).isBefore(Instant.now());
            return new ParsedJob(new JobStatus(key, label, state, updatedAt, exitCode, stale,
                    summary(key, state, output)), output);
        } catch (Exception ignored) {
            return new ParsedJob(new JobStatus(key, label, "unknown", null, null, true, "상태 파일 확인 필요"), "");
        }
    }

    private ServerMetrics metrics(String output) {
        return new ServerMetrics(
                containsResult(output, "App container is healthy"),
                containsResult(output, "Caddy container is running"),
                containsResult(output, "Public HTTPS site is reachable"),
                containsResult(output, "TiDB connection succeeded"),
                findNumber(MEMORY, output), findNumber(DISK, output), findNumber(BACKUP_AGE, output));
    }

    private String summary(String key, String state, String output) {
        if (!"healthy".equals(state)) return "최근 작업 실패 · Discord 알림과 서버 로그를 확인하세요.";
        Matcher matcher = "backup".equals(key) ? BACKUP_SIZE.matcher(output)
                : "offsite-backup".equals(key) ? OFFSITE_FILE.matcher(output) : null;
        if (matcher != null && matcher.find()) {
            return "backup".equals(key) ? "백업 완료 · " + matcher.group(1)
                    : "업로드 완료 · " + matcher.group(1) + " · " + matcher.group(2);
        }
        return "정상 완료";
    }

    private Boolean containsResult(String output, String message) {
        if (output == null || output.isBlank()) return null;
        return output.contains("[OK] " + message + ".") || output.contains("[OK] " + message);
    }

    private Integer findNumber(Pattern pattern, String output) {
        Matcher matcher = pattern.matcher(output == null ? "" : output);
        return matcher.find() ? parseInteger(matcher.group(1)) : null;
    }

    private String decode(String value) {
        if (value == null || value.isBlank()) return "";
        try {
            return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
            return "";
        }
    }

    private Instant parseInstant(String value) {
        try { return value == null ? null : Instant.parse(value); }
        catch (Exception ignored) { return null; }
    }

    private Integer parseInteger(String value) {
        try { return value == null ? null : Integer.valueOf(value); }
        catch (NumberFormatException ignored) { return null; }
    }

    private record ParsedJob(JobStatus view, String output) {}
}
