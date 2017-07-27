package com.github.nikit.cpp.repo;

import com.github.nikit.cpp.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("select p from Post p where p.ownerId = ?#{ principal?.id }")
    Page<Post> findMyPosts(Pageable pageable);

    Page<Post> findByTextContains(Pageable page, String contain);

    Optional<Post> findById(Long id);
}
