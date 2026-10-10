package com.dotani.auth.repository;

import com.dotani.auth.entity.AuthProvider;
import com.dotani.auth.entity.UserAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, String> {

    Optional<UserAccount> findByUsername(String username);

    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findByAuthProviderAndProviderSubject(AuthProvider authProvider, String providerSubject);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
