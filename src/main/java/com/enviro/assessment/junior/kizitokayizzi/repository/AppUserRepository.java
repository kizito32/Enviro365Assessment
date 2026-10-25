package com.enviro.assessment.junior.kizitokayizzi.repository;

import com.enviro.assessment.junior.kizitokayizzi.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);
}
