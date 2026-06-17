package io.greencap.k8s.domain.user;

import io.greencap.k8s.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class UserServiceTest extends PostgresIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void loadUserByUsername_returnsUserDetailsWithCorrectAuthorities() {
        userService.createUser("auth-user", "auth@test.com", "pass",
            Set.of(Permission.GLOBAL_CLUSTERS_VIEW, Permission.WORKLOADS_PODS_VIEW));

        UserDetails details = userService.loadUserByUsername("auth-user");

        assertThat(details.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .containsExactlyInAnyOrder("GLOBAL_CLUSTERS_VIEW", "WORKLOADS_PODS_VIEW");
    }

    @Test
    void loadUserByUsername_withInactiveUser_throwsUsernameNotFoundException() {
        userService.createUser("inactive-user", "inactive@test.com", "pass", Set.of());
        Long userId = userRepository.findByUsername("inactive-user").orElseThrow().getId();
        userService.deactivateUser(userId);

        assertThatThrownBy(() -> userService.loadUserByUsername("inactive-user"))
            .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void createUser_passwordIsStoredAsHash() {
        userService.createUser("hash-user", "hash@test.com", "plainpassword", Set.of());

        UserDetails details = userService.loadUserByUsername("hash-user");

        assertThat(details.getPassword()).isNotEqualTo("plainpassword");
        assertThat(passwordEncoder.matches("plainpassword", details.getPassword())).isTrue();
    }

    @Test
    void loadUserByUsername_withNonExistentUser_throwsUsernameNotFoundException() {
        assertThatThrownBy(() -> userService.loadUserByUsername("does-not-exist"))
            .isInstanceOf(UsernameNotFoundException.class);
    }
}
