package kr.co.jobhub.common.migration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * 운영자가 명시적으로 요청한 한 번의 실행에서 기존 H2 데이터를 현재 MySQL 데이터소스로 복사한다.
 * 대상 DB가 완전히 비어 있지 않으면 시작 전에 중단해 기존 운영 데이터를 덮어쓰지 않는다.
 */
@Component
@Order(-100)
@ConditionalOnProperty(name = "jobhub.migration.h2-to-mysql", havingValue = "true")
public class H2ToMySqlMigrationRunner implements ApplicationRunner {
    private static final List<String> TABLES = List.of(
            "app_users", "login_histories", "job_postings", "qualification_catalog", "institution_compensations", "notices", "data_retention_policies", "admin_audit_logs",
            "user_profiles", "job_alert_preferences", "favorite_organizations", "posting_change_cursors",
            "user_notification_states", "push_subscriptions",
            "scraps", "inquiries", "recruitment_positions", "recruitment_competitions", "posting_changes", "posting_reports");

    private final DataSource targetDataSource;
    private final ConfigurableApplicationContext context;
    private final String sourceUrl;
    private final String sourceUsername;
    private final String sourcePassword;

    public H2ToMySqlMigrationRunner(DataSource targetDataSource, ConfigurableApplicationContext context,
                                    @Value("${jobhub.migration.h2-url:jdbc:h2:file:./data/jobhub;ACCESS_MODE_DATA=r}") String sourceUrl,
                                    @Value("${jobhub.migration.h2-username:sa}") String sourceUsername,
                                    @Value("${jobhub.migration.h2-password:}") String sourcePassword) {
        this.targetDataSource = targetDataSource;
        this.context = context;
        this.sourceUrl = sourceUrl;
        this.sourceUsername = sourceUsername;
        this.sourcePassword = sourcePassword;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!sourceUrl.startsWith("jdbc:h2:"))
            throw new IllegalStateException("이관 원본은 H2 JDBC 주소여야 합니다.");
        Map<String, Long> copied = new LinkedHashMap<>();
        try (Connection source = DriverManager.getConnection(sourceUrl, sourceUsername, sourcePassword);
             Connection target = targetDataSource.getConnection()) {
            if (!target.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT).contains("mysql"))
                throw new IllegalStateException("이관 대상 데이터소스가 MySQL이 아닙니다.");
            prepareTargetSchema(target);
            assertTargetIsEmpty(target);
            target.setAutoCommit(false);
            try {
                execute(target, "SET FOREIGN_KEY_CHECKS=0");
                for (String table : TABLES) {
                    if (!tableExists(source, table)) continue;
                    long rows = copyTable(source, target, table);
                    copied.put(table, rows);
                    System.out.println("H2 -> MySQL: " + table + " " + rows + " rows");
                }
                verifyCounts(source, target, copied);
                execute(target, "SET FOREIGN_KEY_CHECKS=1");
                target.commit();
            } catch (Exception error) {
                target.rollback();
                try {
                    execute(target, "SET FOREIGN_KEY_CHECKS=1");
                } catch (SQLException ignored) {
                    // 원래 이관 오류를 유지한다.
                }
                throw error;
            }
        }
        System.out.println("H2 -> MySQL migration completed: " + copied);
        SpringApplication.exit(context);
    }

    /**
     * 예전 엔티티로 이미 생성된 빈 MySQL 스키마도 최신 장문 데이터 크기에 맞춘다.
     * MySQL DDL은 자동 커밋되므로 데이터 복사를 시작하기 전에만 실행한다.
     */
    private void prepareTargetSchema(Connection target) throws SQLException {
        execute(target, "ALTER TABLE recruitment_positions MODIFY requirements LONGTEXT NULL");
    }

    /** 대상에 한 건이라도 있으면 전체 이관을 거부한다. */
    private void assertTargetIsEmpty(Connection target) throws SQLException {
        for (String table : TABLES) {
            if (!tableExists(target, table))
                throw new IllegalStateException("MySQL 테이블이 없습니다: " + table);
            if (count(target, table) > 0)
                throw new IllegalStateException("MySQL이 비어 있지 않아 이관을 중단했습니다: " + table);
        }
    }

    private long copyTable(Connection source, Connection target, String table) throws Exception {
        Set<String> targetColumns = columns(target, table);
        try (Statement statement = source.createStatement();
             ResultSet rows = statement.executeQuery("SELECT * FROM " + table)) {
            ResultSetMetaData metadata = rows.getMetaData();
            List<String> columns = new ArrayList<>();
            for (int index = 1; index <= metadata.getColumnCount(); index++) {
                String name = metadata.getColumnLabel(index).toLowerCase(Locale.ROOT);
                if (targetColumns.contains(name)) columns.add(name);
            }
            if (columns.isEmpty()) return 0;
            String names = columns.stream().map(name -> "`" + name + "`")
                    .collect(java.util.stream.Collectors.joining(","));
            String values = String.join(",", Collections.nCopies(columns.size(), "?"));
            long count = 0;
            try (PreparedStatement insert = target.prepareStatement(
                    "INSERT INTO `" + table + "` (" + names + ") VALUES (" + values + ")")) {
                while (rows.next()) {
                    for (int index = 0; index < columns.size(); index++) {
                        Object value = rows.getObject(columns.get(index));
                        if (value instanceof Clob clob) value = clob.getSubString(1, Math.toIntExact(clob.length()));
                        if (value instanceof Blob blob) value = blob.getBytes(1, Math.toIntExact(blob.length()));
                        insert.setObject(index + 1, value);
                    }
                    insert.addBatch();
                    count++;
                    if (count % 250 == 0) insert.executeBatch();
                }
                insert.executeBatch();
            }
            return count;
        }
    }

    private void verifyCounts(Connection source, Connection target, Map<String, Long> copied) throws SQLException {
        for (Map.Entry<String, Long> entry : copied.entrySet()) {
            long sourceCount = count(source, entry.getKey());
            long targetCount = count(target, entry.getKey());
            if (sourceCount != targetCount || targetCount != entry.getValue())
                throw new IllegalStateException("이관 건수 검증 실패: " + entry.getKey()
                        + " H2=" + sourceCount + ", MySQL=" + targetCount);
        }
    }

    private Set<String> columns(Connection connection, String table) throws SQLException {
        Set<String> result = new LinkedHashSet<>();
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT * FROM " + table + " WHERE 1=0")) {
            ResultSetMetaData metadata = rows.getMetaData();
            for (int index = 1; index <= metadata.getColumnCount(); index++)
                result.add(metadata.getColumnLabel(index).toLowerCase(Locale.ROOT));
        }
        return result;
    }

    private boolean tableExists(Connection connection, String table) {
        try (Statement statement = connection.createStatement()) {
            statement.executeQuery("SELECT 1 FROM " + table + " WHERE 1=0").close();
            return true;
        } catch (SQLException ignored) {
            return false;
        }
    }

    private long count(Connection connection, String table) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet row = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            row.next();
            return row.getLong(1);
        }
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
