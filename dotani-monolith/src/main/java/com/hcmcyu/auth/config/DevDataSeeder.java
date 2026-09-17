package com.hcmcyu.auth.config;

import com.hcmcyu.auth.entity.Role;
import com.hcmcyu.auth.entity.UserAccount;
import com.hcmcyu.auth.repository.UserAccountRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevDataSeeder implements CommandLineRunner {

    private static final String WARD_ID = "ward-thuong-cat";

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final String demoPassword;

    public DevDataSeeder(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.dev.seed.ward-secretary.password:Demo@12345}") String demoPassword
    ) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.demoPassword = demoPassword;
    }

    @Override
    public void run(String... args) {
        demoUsers().forEach(this::seedUser);
    }

    private void seedUser(DemoUser demoUser) {
        UserAccount user = userAccountRepository.findByUsername(demoUser.username())
                .or(() -> userAccountRepository.findByEmail(demoUser.email()))
                .orElseGet(() -> {
                    UserAccount account = new UserAccount();
                    account.setId(demoUser.userId());
                    account.setUsername(demoUser.username());
                    account.setEmail(demoUser.email());
                    return account;
                });

        user.setPasswordHash(passwordEncoder.encode(demoPassword));
        user.setRole(demoUser.role());
        user.setMemberId(demoUser.memberId());
        user.setOrganizationId(WARD_ID);
        user.setTdpId(demoUser.tdpId());
        user.setEnabled(true);
        userAccountRepository.save(user);
    }

    private List<DemoUser> demoUsers() {
        List<DemoUser> users = new ArrayList<>();
        users.add(new DemoUser(
                "user-admin",
                "admin",
                "admin@hcmcyu.local",
                Role.WARD_SECRETARY,
                "admin-member",
                null
        ));
        users.add(new DemoUser(
                "user-ward-secretary",
                "ward.secretary",
                "ward.secretary@hcmcyu.local",
                Role.WARD_SECRETARY,
                "ward-secretary-member",
                null
        ));
        users.add(new DemoUser(
                "user-ward-deputy",
                "ward.deputy",
                "ward.deputy@hcmcyu.local",
                Role.WARD_DEPUTY_SECRETARY,
                "ward-deputy-member",
                null
        ));

        for (int tdp = 1; tdp <= 5; tdp++) {
            String tdpId = "tdp-" + tdp;
            users.add(new DemoUser(
                    "user-tdp-" + tdp + "-secretary",
                    "tdp" + tdp + ".secretary",
                    "tdp" + tdp + ".secretary@hcmcyu.local",
                    Role.TDP_SECRETARY,
                    "tdp-" + tdp + "-secretary-member",
                    tdpId
            ));
            users.add(new DemoUser(
                    "user-tdp-" + tdp + "-deputy",
                    "tdp" + tdp + ".deputy",
                    "tdp" + tdp + ".deputy@hcmcyu.local",
                    Role.TDP_DEPUTY_SECRETARY,
                    "tdp-" + tdp + "-deputy-member",
                    tdpId
            ));
            for (int member = 1; member <= 5; member++) {
                users.add(new DemoUser(
                        "user-tdp-" + tdp + "-member-" + member,
                        "tdp" + tdp + ".member" + member,
                        "tdp" + tdp + ".member" + member + "@hcmcyu.local",
                        Role.MEMBER,
                        "tdp-" + tdp + "-member-" + member,
                        tdpId
                ));
            }
        }
        return users;
    }

    private record DemoUser(
            String userId,
            String username,
            String email,
            Role role,
            String memberId,
            String tdpId
    ) {
    }
}
