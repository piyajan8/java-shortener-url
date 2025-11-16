package com.macode101.shortenerurl.service;

import com.macode101.shortenerurl.config.ApplicationConfiguration;
import com.macode101.shortenerurl.dto.ShortenUrlResponse;
import com.macode101.shortenerurl.dto.UrlListResponse;
import com.macode101.shortenerurl.entity.ShortenedUrl;
import com.macode101.shortenerurl.exception.ForbiddenException;
import com.macode101.shortenerurl.exception.BadRequestException;
import com.macode101.shortenerurl.exception.ResourceNotFoundException;
import com.macode101.shortenerurl.exception.UrlShortenerException;
import com.macode101.shortenerurl.repository.ShortenedUrlRepository;
import com.macode101.shortenerurl.repository.UserRepository;
import com.macode101.shortenerurl.util.ShortCodeGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UrlServiceImpl implements UrlService {
    
    private static final int MAX_COLLISION_RETRIES = 5;
    
    private final ShortenedUrlRepository shortenedUrlRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final ApplicationConfiguration applicationConfiguration;
    private final UserRepository userRepository;

    public UrlServiceImpl(
            ShortenedUrlRepository shortenedUrlRepository,
            ShortCodeGenerator shortCodeGenerator,
            ApplicationConfiguration applicationConfiguration, UserRepository userRepository
    ) {
        this.shortenedUrlRepository = shortenedUrlRepository;
        this.shortCodeGenerator = shortCodeGenerator;
        this.applicationConfiguration = applicationConfiguration;
        this.userRepository = userRepository;
    }
    
    @Override
    public ShortenUrlResponse createShortUrl(String originalUrl, String userId) {
        if (!userRepository.existsByUid(userId)) {
            throw new ResourceNotFoundException("User not found");
        }

        String shortCode = generateUniqueShortCode();
        
        ShortenedUrl shortenedUrl = new ShortenedUrl();
        shortenedUrl.setShortCode(shortCode);
        shortenedUrl.setOriginalUrl(originalUrl);
        shortenedUrl.setUid(userId);
        shortenedUrl.setActive(true);
        shortenedUrl.setCreatedAt(LocalDateTime.now());
        
        shortenedUrlRepository.save(shortenedUrl);
        
        String shortUrl = applicationConfiguration.getBaseUrl() + "/r/" + shortCode;
        
        return new ShortenUrlResponse(shortUrl, shortCode, originalUrl);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<UrlListResponse> getUserUrls(String userId) {

        List<ShortenedUrl> urls = shortenedUrlRepository.findByUidOrderByCreatedAtDesc(userId);
        
        return urls.stream()
                .map(url -> new UrlListResponse(
                        url.getId(),
                        url.getShortCode(),
                        applicationConfiguration.getBaseUrl() + "/r/" + url.getShortCode(),
                        url.getOriginalUrl(),
                        url.getActive(),
                        url.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }
    
    @Override
    public void deleteUrl(Long id, String userId) {
        ShortenedUrl shortenedUrl = shortenedUrlRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("URL not found with id: " + id));
        
        if (!shortenedUrl.getUid().equals(userId)) {
            throw new ForbiddenException("You do not have permission to delete this URL");
        }
        shortenedUrl.setActive(false);
        shortenedUrlRepository.save(shortenedUrl);
    }
    
    @Override
    @Transactional(readOnly = true)
    public String getOriginalUrl(String shortCode) {
        ShortenedUrl shortenedUrl = shortenedUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Short code not found: " + shortCode));
        
        if (Boolean.FALSE.equals(shortenedUrl.getActive())) {
            throw new BadRequestException("This URL has been deactivated");
        }
        
        return shortenedUrl.getOriginalUrl();
    }

    private String generateUniqueShortCode() {
        for (int attempt = 0; attempt < MAX_COLLISION_RETRIES; attempt++) {
            String shortCode = shortCodeGenerator.generate();
            
            if (!shortenedUrlRepository.existsByShortCode(shortCode)) {
                return shortCode;
            }
        }
        
        throw new UrlShortenerException("Failed to generate unique short code after " + MAX_COLLISION_RETRIES + " attempts");
    }
}
