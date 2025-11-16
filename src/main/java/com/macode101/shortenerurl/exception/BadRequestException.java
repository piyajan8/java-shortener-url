package com.macode101.shortenerurl.exception;

public class BadRequestException extends UrlShortenerException {
    
    public BadRequestException(String message) {
        super(message);
    }
}
