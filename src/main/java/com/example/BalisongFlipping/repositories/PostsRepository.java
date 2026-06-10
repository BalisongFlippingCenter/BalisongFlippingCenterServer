package com.example.BalisongFlipping.repositories;

import com.example.BalisongFlipping.modals.posts.PostWrapper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface PostsRepository extends JpaRepository<PostWrapper, Long>, JpaSpecificationExecutor<PostWrapper> {

    List<PostWrapper> findAllByAccountIdOrderByCreationDateDesc(String accountId);

    void deleteAllByAccountId(String accountId);
}
