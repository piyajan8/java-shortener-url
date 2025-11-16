package com.macode101.shortenerurl.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class Base62ShortCodeGenerator implements ShortCodeGenerator {
    
    private static final String BASE62_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int BASE = BASE62_CHARS.length();
    private static final int MIN_LENGTH = 6;
    private static final int MAX_LENGTH = 8;

    private final SecureRandom secureRandom;

    public Base62ShortCodeGenerator() {
        secureRandom = new SecureRandom();
    }

    @Override
    public String generate() {
        int length = MIN_LENGTH + secureRandom.nextInt(MAX_LENGTH - MIN_LENGTH + 1);

        StringBuilder shortCode = new StringBuilder(length);
        
        for (int i = 0; i < length; i++) {
            int randomIndex = secureRandom.nextInt(BASE);
            shortCode.append(BASE62_CHARS.charAt(randomIndex));
        }
        
        return shortCode.toString();
    }
}
