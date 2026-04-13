package com.culinarycoach.domain.repository;

import com.culinarycoach.domain.entity.User;
import com.culinarycoach.domain.enums.AccountStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameLower(String usernameLower);

    default Optional<User> findByUsernameIgnoreCase(String username) {
        return findByUsernameLower(username.toLowerCase());
    }

    boolean existsByUsernameLower(String usernameLower);

    Page<User> findByStatus(AccountStatus status, Pageable pageable);
}
