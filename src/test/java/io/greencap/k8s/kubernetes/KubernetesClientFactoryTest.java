package io.greencap.k8s.kubernetes;

import io.greencap.k8s.config.EncryptionService;
import io.greencap.k8s.domain.user.User;
import io.greencap.k8s.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KubernetesClientFactoryTest {

    private UserRepository userRepository;
    private EncryptionService encryptionService;
    private KubernetesClientFactory clientFactory;

    @BeforeEach
    void setup() {
        userRepository = mock(UserRepository.class);
        encryptionService = mock(EncryptionService.class);
        clientFactory = new KubernetesClientFactory(encryptionService, userRepository);
    }

    @Test
    void resolveKubeconfigForUser_deniesWhenAuthenticationIsNull() {
        assertThatThrownBy(() -> clientFactory.resolveKubeconfigForUser(null))
                .isInstanceOf(KubernetesOperationException.class)
                .hasMessageContaining("no authenticated user");
    }

    @Test
    void resolveKubeconfigForUser_deniesWhenUserNotFound() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("ghost");
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientFactory.resolveKubeconfigForUser(auth))
                .isInstanceOf(KubernetesOperationException.class)
                .hasMessageContaining("ghost")
                .hasMessageContaining("not found");
    }

    @Test
    void resolveKubeconfigForUser_deniesWhenServiceAccountTokenNotYetProvisioned() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("joao");
        User user = new User();
        user.setUsername("joao");
        user.setServiceaccountToken(null);
        when(userRepository.findByUsername("joao")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> clientFactory.resolveKubeconfigForUser(auth))
                .isInstanceOf(KubernetesOperationException.class)
                .hasMessageContaining("joao")
                .hasMessageContaining("not yet provisioned");
    }

    @Test
    void resolveKubeconfigForUser_returnsDecryptedKubeconfigWhenTokenPresent() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("joao");
        User user = new User();
        user.setUsername("joao");
        user.setServiceaccountToken("encrypted-kubeconfig");
        when(userRepository.findByUsername("joao")).thenReturn(Optional.of(user));
        when(encryptionService.decrypt("encrypted-kubeconfig")).thenReturn("decrypted-kubeconfig");

        String result = clientFactory.resolveKubeconfigForUser(auth);

        assertThat(result).isEqualTo("decrypted-kubeconfig");
    }
}
