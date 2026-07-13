package com.example.BalisongFlipping.repositories;

import com.example.BalisongFlipping.modals.accounts.Account;
import com.example.BalisongFlipping.modals.accounts.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findAccountByEmail(String email);

    List<User> findAllByDisplayName(String displayName);

    Optional<User> findByDisplayNameAndIdentifierCode(String displayName, String identifierCode);

    @Query("SELECT u FROM User u WHERE LOWER(u.displayName) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(u.identifierCode) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<User> searchByDisplayNameOrIdentifierCode(@Param("q") String q);
}
