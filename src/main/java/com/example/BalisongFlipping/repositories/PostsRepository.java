package com.example.BalisongFlipping.repositories;

import com.example.BalisongFlipping.modals.posts.PostWrapper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostsRepository extends JpaRepository<PostWrapper, Long>, JpaSpecificationExecutor<PostWrapper> {

    List<PostWrapper> findAllByAccountIdOrderByCreationDateDesc(String accountId);

    void deleteAllByAccountId(String accountId);

    @Modifying
    @Query("UPDATE PostWrapper p SET p.accountId = NULL WHERE p.accountId = :accountId")
    void anonymizeByAccountId(@Param("accountId") String accountId);
}
