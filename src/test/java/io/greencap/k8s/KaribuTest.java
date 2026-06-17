package io.greencap.k8s;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.util.Arrays;
import java.util.stream.Collectors;

public abstract class KaribuTest {

    @BeforeEach
    void setupVaadin() {
        MockVaadin.setup();
    }

    @AfterEach
    void teardownVaadin() {
        MockVaadin.tearDown();
        SecurityContextHolder.clearContext();
    }

    protected void loginAs(String... authorities) {
        var grantedAuthorities = Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        SecurityContextHolder.setContext(new SecurityContextImpl(
                new UsernamePasswordAuthenticationToken("testuser", null, grantedAuthorities)));
    }
}
