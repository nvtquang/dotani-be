package com.hcmcyu.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hcmcyu.member.entity.Member;
import com.hcmcyu.member.entity.MemberRole;
import com.hcmcyu.member.entity.MemberStatus;
import com.hcmcyu.member.entity.OrganizationUnit;
import com.hcmcyu.member.entity.OrganizationUnitType;
import com.hcmcyu.member.repository.MemberRepository;
import com.hcmcyu.member.repository.MemberRoleChangeAuditRepository;
import com.hcmcyu.member.repository.OrganizationUnitRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:hcmcyu_member_banking_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "storage.local.bank-qr-path=target/test-bank-qr-storage",
        "storage.bank-qr.max-size-bytes=16"
})
class MemberBankingIntegrationTest {

    private static final Path BANK_QR_STORAGE_PATH = Path.of("target/test-bank-qr-storage");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationUnitRepository organizationUnitRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberRoleChangeAuditRepository auditRepository;

    private OrganizationUnit ward;
    private OrganizationUnit tdp1;
    private OrganizationUnit tdp2;
    private Member memberA;
    private Member memberB;
    private Member memberC;

    @BeforeEach
    void setUp() throws Exception {
        cleanStorage();
        auditRepository.deleteAll();
        memberRepository.deleteAll();
        organizationUnitRepository.deleteAll();

        ward = saveOrganization("Phuong Thuong Cat", "THUONG_CAT", OrganizationUnitType.WARD, null);
        tdp1 = saveOrganization("Chi doan TDP 1", "TDP_1", OrganizationUnitType.YOUTH_UNION_BRANCH, ward);
        tdp2 = saveOrganization("Chi doan TDP 2", "TDP_2", OrganizationUnitType.YOUTH_UNION_BRANCH, ward);

        memberA = saveMember("Nguyen Van An", "member-a", tdp1);
        memberB = saveMember("Pham Thi Binh", "member-b", tdp1);
        memberC = saveMember("Tran Van Cuong", "member-c", tdp2);
    }

    @Test
    void memberCanUploadBankQr() throws Exception {
        MockMultipartFile qr = new MockMultipartFile(
                "file",
                "qr.png",
                "image/png",
                new byte[] {1, 2, 3, 4}
        );

        mockMvc.perform(withMemberA(multipart("/api/members/me/banking/qr").file(qr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(memberA.getId()))
                .andExpect(jsonPath("$.bankQrImageUrl").value("/api/members/" + memberA.getId() + "/banking/qr-image"));

        Member updated = memberRepository.findById(memberA.getId()).orElseThrow();
        assertThat(updated.getBankQrImageUrl()).startsWith("/uploads/bank-qr/");
        String storedFilename = updated.getBankQrImageUrl().substring("/uploads/bank-qr/".length());
        assertThat(storedFilename).doesNotContain("qr.png");
        assertThat(Files.exists(BANK_QR_STORAGE_PATH.resolve(storedFilename))).isTrue();

        mockMvc.perform(get(updated.getBankQrImageUrl()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(withMemberA(get("/api/members/{memberId}/banking/qr-image", memberA.getId())))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[] {1, 2, 3, 4}));
    }

    @Test
    void memberCanDeleteBankQr() throws Exception {
        Files.createDirectories(BANK_QR_STORAGE_PATH);
        Path existingQr = BANK_QR_STORAGE_PATH.resolve("existing.png");
        Files.write(existingQr, new byte[] {1, 2, 3});
        memberA.setBankQrImageUrl("/uploads/bank-qr/existing.png");
        memberRepository.save(memberA);

        mockMvc.perform(withMemberA(delete("/api/members/me/banking/qr")))
                .andExpect(status().isNoContent());

        Member updated = memberRepository.findById(memberA.getId()).orElseThrow();
        assertThat(updated.getBankQrImageUrl()).isNull();
        assertThat(Files.exists(existingQr)).isFalse();
    }

    @Test
    void memberCanUpdateBankingInfo() throws Exception {
        mockMvc.perform(withMemberA(put("/api/members/me/banking"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "bankName", "Ngan hang Quan doi",
                                "bankCode", "MB",
                                "accountNumber", "1234567890",
                                "accountHolderName", "NGUYEN VAN AN"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bankName").value("Ngan hang Quan doi"))
                .andExpect(jsonPath("$.bankCode").value("MB"))
                .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                .andExpect(jsonPath("$.accountHolderName").value("NGUYEN VAN AN"));

        Member updated = memberRepository.findById(memberA.getId()).orElseThrow();
        assertThat(updated.getAccountNumber()).isEqualTo("1234567890");
    }

    @Test
    void memberCannotViewAnotherMembersBanking() throws Exception {
        mockMvc.perform(withMemberA(get("/api/members/{memberId}/banking", memberB.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));
    }

    @Test
    void tdpSecretaryCanViewBankingInOwnTdp() throws Exception {
        memberB.setBankName("Vietcombank");
        memberB.setBankCode("VCB");
        memberB.setAccountNumber("555666777");
        memberB.setAccountHolderName("PHAM THI BINH");
        memberB.setBankQrImageUrl("/uploads/bank-qr/existing.png");
        memberRepository.save(memberB);

        mockMvc.perform(withTdpSecretary(get("/api/members/{memberId}/banking", memberB.getId()), tdp1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(memberB.getId()))
                .andExpect(jsonPath("$.accountNumber").value("555666777"))
                .andExpect(jsonPath("$.bankQrImageUrl").value("/api/members/" + memberB.getId() + "/banking/qr-image"));
    }

    @Test
    void bankQrImageEndpointRequiresBankingScope() throws Exception {
        Files.createDirectories(BANK_QR_STORAGE_PATH);
        Files.write(BANK_QR_STORAGE_PATH.resolve("existing.png"), new byte[] {5, 6, 7});
        memberC.setBankQrImageUrl("/uploads/bank-qr/existing.png");
        memberRepository.save(memberC);

        mockMvc.perform(withTdpSecretary(get("/api/members/{memberId}/banking/qr-image", memberC.getId()), tdp1.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));

        mockMvc.perform(withWardSecretary(get("/api/members/{memberId}/banking/qr-image", memberC.getId())))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[] {5, 6, 7}));
    }

    @Test
    void tdpSecretaryCannotViewBankingFromAnotherTdp() throws Exception {
        mockMvc.perform(withTdpSecretary(get("/api/members/{memberId}/banking", memberC.getId()), tdp1.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));
    }

    @Test
    void wardSecretaryCanViewBankingAcrossWard() throws Exception {
        memberC.setBankName("Techcombank");
        memberC.setBankCode("TCB");
        memberC.setAccountNumber("999888777");
        memberC.setAccountHolderName("TRAN VAN CUONG");
        memberRepository.save(memberC);

        mockMvc.perform(withWardSecretary(get("/api/members/{memberId}/banking", memberC.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(memberC.getId()))
                .andExpect(jsonPath("$.accountNumber").value("999888777"));
    }

    @Test
    void memberListDoesNotExposeBankingData() throws Exception {
        memberA.setAccountNumber("1234567890");
        memberA.setBankQrImageUrl("/uploads/bank-qr/existing.png");
        memberRepository.save(memberA);

        mockMvc.perform(withWardSecretary(get("/api/members")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].accountNumber").doesNotExist())
                .andExpect(jsonPath("$.content[0].bankQrImageUrl").doesNotExist());
    }

    @Test
    void bankQrUploadUsesImageValidation() throws Exception {
        MockMultipartFile wrongType = new MockMultipartFile(
                "file",
                "qr.txt",
                "text/plain",
                new byte[] {1, 2, 3}
        );

        mockMvc.perform(withMemberA(multipart("/api/members/me/banking/qr").file(wrongType)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_BANK_QR_FILE"));

        MockMultipartFile tooLarge = new MockMultipartFile(
                "file",
                "qr.webp",
                "image/webp",
                new byte[17]
        );

        mockMvc.perform(withMemberA(multipart("/api/members/me/banking/qr").file(tooLarge)))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("BANK_QR_TOO_LARGE"));
    }

    private OrganizationUnit saveOrganization(
            String name,
            String code,
            OrganizationUnitType type,
            OrganizationUnit parent
    ) {
        OrganizationUnit organizationUnit = new OrganizationUnit();
        organizationUnit.setName(name);
        organizationUnit.setCode(code);
        organizationUnit.setType(type);
        organizationUnit.setParent(parent);
        return organizationUnitRepository.save(organizationUnit);
    }

    private Member saveMember(String fullName, String userId, OrganizationUnit organization) {
        Member member = new Member();
        member.setFullName(fullName);
        member.setUserId(userId);
        member.setEmail(userId + "@example.com");
        member.setMemberStatus(MemberStatus.ACTIVE);
        member.setMemberRole(MemberRole.MEMBER);
        member.setOrganization(organization);
        return memberRepository.save(member);
    }

    private MockHttpServletRequestBuilder withMemberA(MockHttpServletRequestBuilder request) {
        return request
                .header("X-User-Id", memberA.getUserId())
                .header("X-Member-Id", memberA.getId())
                .header("X-User-Role", "MEMBER")
                .header("X-Organization-Id", ward.getId())
                .header("X-Tdp-Id", tdp1.getId());
    }

    private MockHttpServletRequestBuilder withTdpSecretary(
            MockHttpServletRequestBuilder request,
            String tdpId
    ) {
        return request
                .header("X-User-Id", "tdp-secretary-user")
                .header("X-User-Role", "TDP_SECRETARY")
                .header("X-Organization-Id", ward.getId())
                .header("X-Tdp-Id", tdpId);
    }

    private MockHttpServletRequestBuilder withWardSecretary(MockHttpServletRequestBuilder request) {
        return request
                .header("X-User-Id", "ward-secretary-user")
                .header("X-User-Role", "WARD_SECRETARY")
                .header("X-Organization-Id", ward.getId());
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private void cleanStorage() throws Exception {
        if (!Files.exists(BANK_QR_STORAGE_PATH)) {
            return;
        }
        try (var files = Files.walk(BANK_QR_STORAGE_PATH)) {
            files.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    });
        }
    }
}
