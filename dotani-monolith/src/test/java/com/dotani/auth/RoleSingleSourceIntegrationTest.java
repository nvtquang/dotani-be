package com.dotani.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dotani.auth.repository.UserAccountRepository;
import com.dotani.member.entity.Member;
import com.dotani.member.entity.MemberRole;
import com.dotani.support.IntegrationTest;
import com.dotani.support.TestData.Fixture;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Role and scope are stored only on the member; tokens are always derived from it. */
class RoleSingleSourceIntegrationTest extends IntegrationTest {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void roleChangeOnMemberIsCarriedByTheNextIssuedToken() throws Exception {
        Fixture f = data.standardFixture();
        Member member = f.tdp1Member();
        var account = userAccountRepository.findById(member.getUserId()).orElseThrow();
        account.setPasswordHash(passwordEncoder.encode("Password@123"));
        userAccountRepository.save(account);

        var login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("usernameOrEmail", account.getUsername(), "password", "Password@123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("MEMBER"))
                .andExpect(jsonPath("$.user.organizationId").value(f.ward().getId()))
                .andExpect(jsonPath("$.user.tdpId").value(f.tdp1().getId()))
                .andReturn();
        String refreshToken = body(login).get("refreshToken").asText();

        mockMvc.perform(auth.as(f.wardSecretary(), put("/api/members/{id}/role", member.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("role", MemberRole.TDP_SECRETARY.name()))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("TDP_SECRETARY"))
                .andExpect(jsonPath("$.user.tdpId").value(f.tdp1().getId()));
    }

    @Test
    void wardOfficerProfileInsideATdpStillGetsWardScope() throws Exception {
        Fixture f = data.standardFixture();
        Member officer = data.member("Can bo phuong o TDP", f.tdp1(), MemberRole.WARD_DEPUTY_SECRETARY);
        var account = userAccountRepository.findById(officer.getUserId()).orElseThrow();
        account.setPasswordHash(passwordEncoder.encode("Password@123"));
        userAccountRepository.save(account);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("usernameOrEmail", account.getUsername(), "password", "Password@123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("WARD_DEPUTY_SECRETARY"))
                .andExpect(jsonPath("$.user.organizationId").value(f.ward().getId()))
                .andExpect(jsonPath("$.user.tdpId").doesNotExist());
    }
}
