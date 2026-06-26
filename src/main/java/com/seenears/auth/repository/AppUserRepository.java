package com.seenears.auth.repository;

import com.seenears.auth.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    boolean existsByPhoneNumber(String phoneNumber);

    Optional<AppUser> findByPhoneNumber(String phoneNumber);
}
