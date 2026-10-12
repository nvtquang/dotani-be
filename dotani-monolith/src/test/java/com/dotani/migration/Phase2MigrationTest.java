package com.dotani.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.dotani.member.migration.V806__Move_banking_to_member_banking;
import com.dotani.shared.crypto.FieldEncryptor;
import java.util.List;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * Runs the phase-2 migrations (V801..V809) over data in the pre-phase-2 shape (V704) and checks every
 * transformation. Uses its own database (dotani_migration_test) because it drops and rebuilds the schema.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Phase2MigrationTest {

    private static final String KEY = "8VgSHMFfze2Tu5Ymv22tg6mW8WVIPNmFfk/zQ9el6o0=";

    private final FieldEncryptor encryptor = new FieldEncryptor(KEY);
    private JdbcTemplate jdbc;

    @BeforeAll
    void migrateLegacyData() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                env("TEST_MIGRATION_DB_URL",
                        "jdbc:mysql://127.0.0.1:3306/dotani_migration_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"),
                env("TEST_DB_USERNAME", "dotani_test"),
                env("TEST_DB_PASSWORD", "dotani_test")
        );
        jdbc = new JdbcTemplate(dataSource);

        flyway(dataSource, "704").clean();
        flyway(dataSource, "704").migrate();
        insertLegacyData();
        flyway(dataSource, null).migrate();
    }

    private Flyway flyway(DriverManagerDataSource dataSource, String target) {
        var config = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .placeholders(Map.of("legacy_time_zone", "+07:00"))
                .javaMigrations(new V806__Move_banking_to_member_banking(encryptor))
                .cleanDisabled(false);
        if (target != null) {
            config.target(target);
        }
        return config.load();
    }

    private void insertLegacyData() {
        jdbc.batchUpdate(
                "INSERT INTO organization_units VALUES ('ward', 'Phuong', 'WARD', 'WARD', NULL, TRUE, '2026-10-01 15:00:00', '2026-10-01 15:00:00')",
                "INSERT INTO organization_units VALUES ('tdp-1', 'TDP 1', 'TDP_1', 'YOUTH_UNION_BRANCH', 'ward', TRUE, '2026-10-01 15:00:00', '2026-10-01 15:00:00')",
                // accounts (role and scope still stored here before V802)
                "INSERT INTO user_accounts (id, username, email, password_hash, role, member_id, organization_id, tdp_id, enabled, created_at, updated_at)"
                        + " VALUES ('u-a', 'a', 'a@x.vn', 'h', 'MEMBER', 'm-a', 'ward', 'tdp-1', TRUE, '2026-10-01 15:00:00', '2026-10-01 15:00:00')",
                "INSERT INTO user_accounts (id, username, email, password_hash, role, member_id, organization_id, tdp_id, enabled, created_at, updated_at)"
                        + " VALUES ('u-b', 'b', 'b@x.vn', 'h', 'MEMBER', 'm-b', 'ward', 'tdp-1', TRUE, '2026-10-01 15:00:00', '2026-10-01 15:00:00')",
                // m-a: same email as the account; m-b: edited profile email; m-c: no account, contact email
                "INSERT INTO members (id, user_id, full_name, email, member_status, member_role, organization_id, created_at, updated_at,"
                        + " bank_name, bank_code, account_number, account_holder_name, bank_qr_image_url)"
                        + " VALUES ('m-a', 'u-a', 'A', 'A@X.VN', 'ACTIVE', 'MEMBER', 'tdp-1', '2026-10-01 15:00:00', '2026-10-01 15:00:00',"
                        + " 'VCB', 'VCB', '0123456789', 'NGUYEN A', '/uploads/bank-qr/a.png')",
                "INSERT INTO members (id, user_id, full_name, email, member_status, member_role, organization_id, created_at, updated_at)"
                        + " VALUES ('m-b', 'u-b', 'B', 'new-b@x.vn', 'ACTIVE', 'TDP_SECRETARY', 'tdp-1', '2026-10-01 15:00:00', '2026-10-01 15:00:00')",
                "INSERT INTO members (id, user_id, full_name, email, member_status, member_role, organization_id, created_at, updated_at)"
                        + " VALUES ('m-c', NULL, 'C', 'c@x.vn', 'ACTIVE', 'MEMBER', 'tdp-1', '2026-10-01 15:00:00', '2026-10-01 15:00:00')",
                // event at 08:00 Vietnam time = 01:00 UTC
                "INSERT INTO events VALUES ('e-1', 'Hop', NULL, 'MEETING', NULL, '2026-12-01 08:00:00', '2026-12-01 10:00:00', NULL,"
                        + " 'tdp-1', NULL, 'PUBLISHED', 'm-b', '2026-10-01 15:00:00', '2026-10-01 15:00:00')",
                "INSERT INTO event_attachments VALUES ('ea-1', 'e-1', '/uploads/event-attachments/x.pdf', 'FILE', 'ke-hoach.pdf', '2026-10-01 15:00:00')",
                "INSERT INTO posts VALUES ('p-1', 'Bai', 'Noi dung', 'NEWS', 'tdp-1', 'm-b', 'PUBLISHED', '2026-10-01 15:00:00', '2026-10-01 15:00:00')",
                "INSERT INTO post_images (id, post_id, image_url, created_at, attachment_kind, file_name)"
                        + " VALUES ('pi-1', 'p-1', '/uploads/post-images/y.png', '2026-10-01 15:00:00', 'IMAGE', 'anh.png')",
                "INSERT INTO conversations (id, type, title, created_by, created_at, updated_at) VALUES ('c-1', 'DIRECT', NULL, 'm-a', '2026-10-01 15:00:00', '2026-10-01 15:00:00')",
                "INSERT INTO conversation_members VALUES ('cm-1', 'c-1', 'm-a', '2026-10-01 15:00:00', '2026-10-01 15:00:00')",
                "INSERT INTO conversation_members VALUES ('cm-2', 'c-1', 'm-b', '2026-10-01 15:00:00', NULL)",
                "INSERT INTO messages VALUES ('msg-1', 'c-1', 'm-a', 'bao-cao.pdf', '2026-10-01 15:00:00',"
                        + " '/uploads/chat/attachments/z.pdf', 'bao-cao.pdf', 'application/pdf', 1234, 'FILE')",
                "INSERT INTO messages (id, conversation_id, sender_id, content, created_at) VALUES ('msg-2', 'c-1', 'm-b', 'xin chao', '2026-10-01 15:00:00')",
                "INSERT INTO notifications VALUES ('n-1', 'SYSTEM', 'Chao', 'Noi dung', NULL, NULL, '2026-10-01 15:00:00')",
                "INSERT INTO user_notifications VALUES ('un-1', 'n-1', 'm-a', NULL, '2026-10-01 15:00:00')",
                // orphan: points at a member that does not exist
                "INSERT INTO user_notifications VALUES ('un-orphan', 'n-1', 'ghost', NULL, '2026-10-01 15:00:00')",
                "INSERT INTO audit_logs VALUES ('al-1', 'u-b', 'TDP_SECRETARY', 'CREATE_POST', 'POST', 'p-1', 'tdp-1', '2026-10-01 15:00:00', 'SUCCESS')"
        );
    }

    @Test
    void v801DropsOrphanLinkRowsAndKeepsTheRest() {
        assertThat(ids("select id from user_notifications")).containsExactly("un-1");
        assertThat(count("select count(*) from information_schema.table_constraints where table_schema = database()"
                + " and constraint_type = 'FOREIGN KEY' and constraint_name = 'fk_messages_sender_id'")).isEqualTo(1);
    }

    @Test
    void v802RemovesRoleAndScopeFromAccounts() {
        assertThat(columns("user_accounts")).doesNotContain("role", "organization_id", "tdp_id");
        assertThat(jdbc.queryForObject("select member_role from members where id = 'm-b'", String.class))
                .isEqualTo("TDP_SECRETARY");
    }

    @Test
    void v803StoresEachEmailOnceAndBacksUpDifferingOnes() {
        assertThat(jdbc.queryForObject("select email from members where id = 'm-a'", String.class)).isNull();
        assertThat(jdbc.queryForObject("select email from members where id = 'm-b'", String.class)).isNull();
        assertThat(jdbc.queryForObject("select email from members where id = 'm-c'", String.class)).isEqualTo("c@x.vn");
        assertThat(jdbc.queryForMap("select member_id, email, account_email from member_email_backup"))
                .containsEntry("member_id", "m-b")
                .containsEntry("email", "new-b@x.vn")
                .containsEntry("account_email", "b@x.vn");
    }

    @Test
    void v804ConvertsVietnamWallTimeToUtc() {
        assertThat(time("select start_time from events where id = 'e-1'")).isEqualTo("2026-12-01 01:00:00");
        assertThat(time("select created_at from members where id = 'm-a'")).isEqualTo("2026-10-01 08:00:00");
        assertThat(time("select last_read_at from conversation_members where id = 'cm-1'")).isEqualTo("2026-10-01 08:00:00");
        assertThat(time("select audit_timestamp from audit_logs where id = 'al-1'")).isEqualTo("2026-10-01 08:00:00");
        assertThat(jdbc.queryForObject("select data_type from information_schema.columns where table_schema = database()"
                + " and table_name = 'events' and column_name = 'start_time'", String.class)).isEqualTo("datetime");
    }

    @Test
    void v805MovesEveryAttachmentIntoOneTableKeepingIds() {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select id, owner_type, owner_id, kind, name, mime, size, created_by from attachments order by owner_type");
        assertThat(rows).hasSize(3);
        assertThat(rows.get(0)).containsEntry("id", "ea-1").containsEntry("owner_type", "EVENT").containsEntry("owner_id", "e-1");
        assertThat(rows.get(1)).containsEntry("owner_type", "MESSAGE").containsEntry("owner_id", "msg-1")
                .containsEntry("mime", "application/pdf").containsEntry("size", 1234L).containsEntry("created_by", "m-a");
        assertThat(rows.get(2)).containsEntry("id", "pi-1").containsEntry("owner_type", "POST").containsEntry("name", "anh.png");
        assertThat(tables()).doesNotContain("event_attachments", "post_images");
        assertThat(columns("messages")).doesNotContain("attachment_url", "attachment_kind");
    }

    @Test
    void v806MovesBankingAndEncryptsTheAccountNumber() {
        Map<String, Object> banking = jdbc.queryForMap("select * from member_banking where member_id = 'm-a'");
        assertThat(banking).containsEntry("bank_name", "VCB").containsEntry("bank_qr_image_url", "/uploads/bank-qr/a.png");
        String encrypted = (String) banking.get("account_number_encrypted");
        assertThat(encrypted).startsWith("v1:").doesNotContain("0123456789");
        assertThat(encryptor.decrypt(encrypted)).isEqualTo("0123456789");
        assertThat(count("select count(*) from member_banking")).isEqualTo(1);
        assertThat(columns("members")).doesNotContain("account_number", "bank_name");
    }

    @Test
    void v807ToV809AddAuditColumnsSoftDeleteAndIndexes() {
        assertThat(columns("audit_logs")).contains("details", "ip", "user_agent");
        assertThat(columns("members")).contains("deleted_at");
        assertThat(columns("posts")).contains("deleted_at");
        assertThat(columns("events")).contains("deleted_at");
        assertThat(count("select count(*) from information_schema.statistics where table_schema = database()"
                + " and index_name in ('idx_posts_org_status_created', 'idx_events_org_status_created')")).isGreaterThan(0);
    }

    @Test
    void noBusinessRowIsLost() {
        assertThat(count("select count(*) from members")).isEqualTo(3);
        assertThat(count("select count(*) from user_accounts")).isEqualTo(2);
        assertThat(count("select count(*) from events")).isEqualTo(1);
        assertThat(count("select count(*) from posts")).isEqualTo(1);
        assertThat(count("select count(*) from messages")).isEqualTo(2);
        assertThat(count("select count(*) from audit_logs")).isEqualTo(1);
    }

    private List<String> ids(String sql) {
        return jdbc.queryForList(sql, String.class);
    }

    private long count(String sql) {
        return jdbc.queryForObject(sql, Long.class);
    }

    private String time(String sql) {
        return jdbc.queryForObject("select date_format((" + sql + "), '%Y-%m-%d %H:%i:%s')", String.class);
    }

    private List<String> columns(String table) {
        return jdbc.queryForList("select column_name from information_schema.columns where table_schema = database()"
                + " and table_name = ?", String.class, table);
    }

    private List<String> tables() {
        return jdbc.queryForList("select table_name from information_schema.tables where table_schema = database()", String.class);
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
