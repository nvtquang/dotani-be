package com.dotani.member.migration;

import com.dotani.shared.crypto.FieldEncryptor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.stereotype.Component;

/**
 * Moves bank details from members into member_banking and encrypts account numbers (AES-GCM).
 * Written in Java because encryption needs the application key; Spring Boot hands Flyway every
 * JavaMigration bean, so the encryptor is injected here.
 */
@Component
public class V806__Move_banking_to_member_banking extends BaseJavaMigration {

    private final FieldEncryptor fieldEncryptor;

    public V806__Move_banking_to_member_banking(FieldEncryptor fieldEncryptor) {
        this.fieldEncryptor = fieldEncryptor;
    }

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE member_banking (
                        member_id VARCHAR(36) NOT NULL,
                        bank_name VARCHAR(255) NULL,
                        bank_code VARCHAR(50) NULL,
                        account_number_encrypted VARCHAR(512) NULL,
                        account_holder_name VARCHAR(255) NULL,
                        bank_qr_image_url VARCHAR(1000) NULL,
                        created_at DATETIME(6) NOT NULL,
                        updated_at DATETIME(6) NOT NULL,
                        PRIMARY KEY (member_id),
                        CONSTRAINT fk_member_banking_member_id FOREIGN KEY (member_id) REFERENCES members (id)
                    )
                    """);
        }

        String select = """
                SELECT id, bank_name, bank_code, account_number, account_holder_name, bank_qr_image_url,
                       created_at, updated_at
                FROM members
                WHERE bank_name IS NOT NULL OR bank_code IS NOT NULL OR account_number IS NOT NULL
                   OR account_holder_name IS NOT NULL OR bank_qr_image_url IS NOT NULL
                """;
        String insert = """
                INSERT INTO member_banking (member_id, bank_name, bank_code, account_number_encrypted,
                                            account_holder_name, bank_qr_image_url, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(select);
             PreparedStatement insertRow = connection.prepareStatement(insert)) {
            while (rows.next()) {
                insertRow.setString(1, rows.getString("id"));
                insertRow.setString(2, rows.getString("bank_name"));
                insertRow.setString(3, rows.getString("bank_code"));
                insertRow.setString(4, fieldEncryptor.encrypt(rows.getString("account_number")));
                insertRow.setString(5, rows.getString("account_holder_name"));
                insertRow.setString(6, rows.getString("bank_qr_image_url"));
                // copy timestamps as stored (already UTC after V804), without client time zone conversion
                insertRow.setObject(7, rows.getObject("created_at"));
                insertRow.setObject(8, rows.getObject("updated_at"));
                insertRow.addBatch();
            }
            insertRow.executeBatch();
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    ALTER TABLE members
                        DROP COLUMN bank_name,
                        DROP COLUMN bank_code,
                        DROP COLUMN account_number,
                        DROP COLUMN account_holder_name,
                        DROP COLUMN bank_qr_image_url
                    """);
        }
    }
}
