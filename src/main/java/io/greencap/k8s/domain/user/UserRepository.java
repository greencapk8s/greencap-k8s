package io.greencap.k8s.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.activeCluster WHERE u.username = :username")
    Optional<User> findByUsernameWithActiveCluster(@Param("username") String username);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.activeCluster ORDER BY u.createdAt ASC")
    List<User> findAllWithActiveCluster();

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
