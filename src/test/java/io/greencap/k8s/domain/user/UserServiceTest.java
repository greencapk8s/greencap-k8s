package io.greencap.k8s.domain.user;

import io.greencap.k8s.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

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
    void loadUserByUsername_returnsUserDetailsWithRoleUser() {
        userService.createUser("auth-user", "auth@test.com", "pass");

        UserDetails details = userService.loadUserByUsername("auth-user");

        assertThat(details.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .containsExactly("ROLE_USER");
    }

    @Test
    void loadUserByUsername_withDeletedUser_throwsUsernameNotFoundException() {
        userService.createUser("deleted-user", "deleted@test.com", "pass");
        Long userId = userRepository.findByUsername("deleted-user").orElseThrow().getId();
        userService.deleteUser(userId);

        assertThatThrownBy(() -> userService.loadUserByUsername("deleted-user"))
            .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void createUser_passwordIsStoredAsHash() {
        userService.createUser("hash-user", "hash@test.com", "plainpassword");

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
