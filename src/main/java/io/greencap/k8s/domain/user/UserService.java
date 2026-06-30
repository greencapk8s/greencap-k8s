package io.greencap.k8s.domain.user;

import io.greencap.k8s.domain.cluster.Cluster;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .filter(User::isActive)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                .build();
    }

    @Transactional
    public User createUser(String username, String email, String rawPassword) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        return userRepository.save(user);
    }

    @Transactional
    public void assignKubernetesIdentity(Long userId, String serviceaccountName, String clusterRoleName, String encryptedToken) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setServiceaccountName(serviceaccountName);
            user.setClusterRoleName(clusterRoleName);
            user.setServiceaccountToken(encryptedToken);
            userRepository.save(user);
        });
    }

    @Transactional
    public void clearKubernetesIdentity(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setServiceaccountName(null);
            user.setClusterRoleName(null);
            user.setServiceaccountToken(null);
            userRepository.save(user);
        });
    }

    @Transactional
    public void updateActiveCluster(String username, Cluster cluster) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setActiveCluster(cluster);
            userRepository.save(user);
        });
    }

    public Optional<Cluster> findActiveCluster(String username) {
        return userRepository.findByUsernameWithActiveCluster(username)
                .map(User::getActiveCluster);
    }

    @Transactional
    public void updateActiveNamespace(String username, String namespace) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setActiveNamespace(namespace);
            userRepository.save(user);
        });
    }

    public Optional<String> findActiveNamespace(String username) {
        return userRepository.findByUsername(username)
                .map(User::getActiveNamespace);
    }

    public Optional<Integer> findRefreshInterval(String username) {
        return userRepository.findByUsername(username)
                .map(User::getRefreshIntervalSeconds);
    }

    @Transactional
    public void updateRefreshInterval(String username, int seconds) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setRefreshIntervalSeconds(seconds);
            userRepository.save(user);
        });
    }

    public Optional<Integer> findDrawerWidth(String username) {
        return userRepository.findByUsername(username)
                .map(User::getDrawerWidthPx);
    }

    @Transactional
    public void updateDrawerWidth(String username, int width) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setDrawerWidthPx(width);
            userRepository.save(user);
        });
    }

    public Optional<String> findTheme(String username) {
        return userRepository.findByUsername(username)
                .map(User::getTheme);
    }

    @Transactional
    public void updateTheme(String username, String theme) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setTheme(theme);
            userRepository.save(user);
        });
    }

    public List<User> findAll() {
        return userRepository.findAllWithActiveCluster();
    }

    @Transactional
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }

    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }
}
