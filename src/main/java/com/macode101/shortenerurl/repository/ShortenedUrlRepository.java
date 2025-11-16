package com.macode101.shortenerurl.repository;

import com.macode101.shortenerurl.entity.ShortenedUrl;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShortenedUrlRepository extends JpaRepository<ShortenedUrl, Long> {
    
    Optional<ShortenedUrl> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    List<ShortenedUrl> findByUidOrderByCreatedAtDesc(String user);
}
