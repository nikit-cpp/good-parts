package com.github.nikit.cpp.repo.jpa;

import com.github.nikit.cpp.entity.jpa.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsername(String username);

    Optional<UserAccount> findById(Long id);

    Optional<UserAccount> findByEmail(String email);

    Page<UserAccount> findByUsernameContains(Pageable springDataPage, String login);
}
