package com.example.JustBuyIt.Configurations;

import com.example.JustBuyIt.DTOs.ApiError;
import com.example.JustBuyIt.Exceptions.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;


@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Always force application/json. Without this, if a controller (e.g. the
     * product image endpoint) already set Content-Type to image/jpeg before
     * failing, Spring cannot write ApiError and throws
     * HttpMessageNotWritableException.
     */
    private ResponseEntity<ApiError> build(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ApiError(code, message));
    }

    // 1. Our deliberate, client-safe errors
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException e) {
        return build(e.getStatus(), e.getStatus().name(), e.getMessage());
    }

    // 2. Product-rule violations (quota, ownership) — deliberate, safe to expose
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(IllegalStateException e) {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN", e.getMessage());
    }

    // 3. Bad input
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArg(IllegalArgumentException e) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", e.getMessage());
    }

    // 4. Not authenticated
    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<ApiError> handleUnauth(AuthenticationCredentialsNotFoundException e) {
        return build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Please log in to continue.");
    }

    // 5. Existing code uses this for "product not found" — map it to 404
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiError> handleUserNotFound(UsernameNotFoundException e) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", e.getMessage());
    }

    // 6. Fallback for RuntimeException AND any other unchecked exception.
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> handleRuntime(RuntimeException e) {
        log.error("Unhandled RuntimeException", e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER_ERROR",
                "Something went wrong. Please try again.");
    }

    // 7. Belt-and-suspenders for checked exceptions that slip through
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAny(Exception e) {
        log.error("Unhandled Exception", e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER_ERROR",
                "Something went wrong. Please try again.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException e) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        String msg = String.format("Request method '%s' is not supported. Allowed methods: %s",
                e.getMethod(), e.getSupportedHttpMethods());
        return build(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", msg);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUpload(MaxUploadSizeExceededException e) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "PAYLOAD_TOO_LARGE",
                "Image is too large. Please upload a file smaller than 10 MB.");
    }
}