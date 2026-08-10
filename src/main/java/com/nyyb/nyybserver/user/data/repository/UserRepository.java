package com.nyyb.nyybserver.user.data.repository;

import com.nyyb.nyybserver.user.data.entity.User;
import com.nyyb.nyybserver.user.data.enums.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);

    Optional<User> findByIdAndProvider(Long id, AuthProvider provider);
}
