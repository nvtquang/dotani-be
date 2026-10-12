package com.dotani.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dotani.shared.crypto.FieldEncryptor;
import com.dotani.support.IntegrationTest;
import com.dotani.support.TestData.Fixture;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

/** Bank details live in member_banking and the account number is encrypted at rest. */
class BankingEncryptionIntegrationTest extends IntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FieldEncryptor fieldEncryptor;

    @Test
    void accountNumberIsEncryptedInTheDatabaseButReadableThroughTheApi() throws Exception {
        Fixture f = data.standardFixture();

        mockMvc.perform(auth.as(f.tdp1Member(), put("/api/members/me/banking"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "bankName", "Vietcombank",
                                "bankCode", "VCB",
                                "accountNumber", "0123456789",
                                "accountHolderName", "DOAN VIEN TDP 1"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("0123456789"));

        String stored = jdbcTemplate.queryForObject(
                "select account_number_encrypted from member_banking where member_id = ?",
                String.class, f.tdp1Member().getId());
        assertThat(stored).startsWith("v1:").doesNotContain("0123456789");
        assertThat(fieldEncryptor.decrypt(stored)).isEqualTo("0123456789");

        mockMvc.perform(auth.as(f.tdp1Member(), get("/api/members/me/banking")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("0123456789"))
                .andExpect(jsonPath("$.bankName").value("Vietcombank"));
    }

    @Test
    void encryptingTheSameValueTwiceGivesDifferentCiphertexts() {
        assertThat(fieldEncryptor.encrypt("0123456789")).isNotEqualTo(fieldEncryptor.encrypt("0123456789"));
    }

    @Test
    void membersWithoutBankDetailsHaveNoBankingRow() {
        Fixture f = data.standardFixture();
        Integer rows = jdbcTemplate.queryForObject(
                "select count(*) from member_banking where member_id = ?", Integer.class, f.tdp2Member().getId());
        assertThat(rows).isZero();
    }
}
