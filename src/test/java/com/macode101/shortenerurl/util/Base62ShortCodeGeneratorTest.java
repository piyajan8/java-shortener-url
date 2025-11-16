package com.macode101.shortenerurl.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Base62ShortCodeGeneratorTest {

    private Base62ShortCodeGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new Base62ShortCodeGenerator();
    }

    @Test
    void generateShouldReturnNonNullCode() {
        String shortCode = generator.generate();
        assertNotNull(shortCode);
    }

    @Test
    void generateShouldReturnCodeWithValidLength() {
        String shortCode = generator.generate();

        assertTrue(shortCode.length() >= 6 && shortCode.length() <= 8,
                "Short code length should be between 6 and 8 characters, but was: " + shortCode.length());
    }

    @Test
    void generateShouldReturnCodeWithBase62Characters() {
        String validChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

        String shortCode = generator.generate();

        for (char c : shortCode.toCharArray()) {
            assertTrue(validChars.indexOf(c) >= 0,
                    "Short code contains invalid character: " + c);
        }
    }

    @Test
    void generateShouldNotContainSpecialCharacters() {
        for (int i = 0; i < 100; i++) {
            String shortCode = generator.generate();

            assertFalse(shortCode.contains("-"));
            assertFalse(shortCode.contains("_"));
            assertFalse(shortCode.contains(" "));
            assertFalse(shortCode.matches(".*[^a-zA-Z0-9].*"));
        }
    }
}
