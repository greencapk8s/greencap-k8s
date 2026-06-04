package io.greencap.k8s.config;

import com.vaadin.flow.spring.security.VaadinWebSecurity;
import io.greencap.k8s.domain.user.UserService;
import io.greencap.k8s.ui.LoginView;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig extends VaadinWebSecurity {

    private final UserService userService;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(new AntPathRequestMatcher("/greencap.png")).permitAll()
        );
        super.configure(http);
        setLoginView(http, LoginView.class);
    }
}
