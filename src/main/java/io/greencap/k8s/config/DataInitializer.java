package io.greencap.k8s.config;

import io.greencap.k8s.domain.user.Role;
import io.greencap.k8s.domain.user.UserRepository;
import io.greencap.k8s.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final UserService userService;

    @Override
    public void run(ApplicationArguments args) {
        if (!userRepository.existsByUsername("admin")) {
            userService.createUser("admin", "admin@greencap.local", "admin", Role.ADMIN);
            log.info("Admin user created — login: admin / admin");
        }
    }
}
