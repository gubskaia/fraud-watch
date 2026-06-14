package com.fraudwatch.auth.repository;

import com.fraudwatch.auth.domain.RefreshToken;
import com.fraudwatch.auth.domain.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    List<RefreshToken> findAllByUser(User user);

    Optional<RefreshToken> findByToken(String token);

    void deleteAllByExpiresAtBefore(Instant instant);
}

