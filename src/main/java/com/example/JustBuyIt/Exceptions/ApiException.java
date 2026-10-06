package com.example.JustBuyIt.Exceptions;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }

    // convenience factories
    public static ApiException notFound(String msg)  { return new ApiException(HttpStatus.NOT_FOUND, msg); }
    public static ApiException badRequest(String msg){ return new ApiException(HttpStatus.BAD_REQUEST, msg); }
    public static ApiException forbidden(String msg) { return new ApiException(HttpStatus.FORBIDDEN, msg); }
    public static ApiException conflict(String msg)  { return new ApiException(HttpStatus.CONFLICT, msg); }
}