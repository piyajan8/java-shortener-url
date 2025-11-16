package com.macode101.shortenerurl.exception;

public class ForbiddenException extends UrlShortenerException {
    
    public ForbiddenException(String message) {
        super(message);
    }
}
