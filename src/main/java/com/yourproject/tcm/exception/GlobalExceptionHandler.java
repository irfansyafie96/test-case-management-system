package com.yourproject.tcm.exception;

import com.yourproject.tcm.model.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Global exception handler for consistent error responses across all REST API endpoints.
 * Catches unhandled exceptions and returns structured ErrorResponse objects.
 */
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * Handle generic Exception (catch-all for unhandled exceptions)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception ex,
            HttpServletRequest request) {
        
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String error = "Internal Server Error";
        
        // Map common runtime exceptions to appropriate HTTP status codes
        if (ex instanceof RuntimeException) {
            // Check exception message for common patterns
            String message = ex.getMessage();
            if (message != null) {
                if (message.contains("not found") || message.contains("does not exist")) {
                    status = HttpStatus.NOT_FOUND;
                    error = "Not Found";
                } else if (message.contains("already exists") || message.contains("duplicate")) {
                    status = HttpStatus.CONFLICT;
                    error = "Conflict";
                } else if (message.contains("validation") || message.contains("invalid")) {
                    status = HttpStatus.BAD_REQUEST;
                    error = "Bad Request";
                } else {
                    status = HttpStatus.BAD_REQUEST;
                    error = "Bad Request";
                }
            } else {
                status = HttpStatus.BAD_REQUEST;
                error = "Bad Request";
            }
        }
        
        ErrorResponse errorResponse = new ErrorResponse(
                status.value(),
                error,
                ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred",
                request.getRequestURI()
        );
        
        return new ResponseEntity<>(errorResponse, status);
    }

    /**
     * Handle RuntimeException specifically for more fine-grained control
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex,
            HttpServletRequest request) {
        
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String error = "Bad Request";
        
        String message = ex.getMessage();
        if (message != null) {
            if (message.contains("not found") || message.contains("does not exist")) {
                status = HttpStatus.NOT_FOUND;
                error = "Not Found";
            } else if (message.contains("already exists") || message.contains("duplicate")) {
                status = HttpStatus.CONFLICT;
                error = "Conflict";
            } else if (message.contains("validation") || message.contains("invalid")) {
                status = HttpStatus.BAD_REQUEST;
                error = "Bad Request";
            }
        }
        
        ErrorResponse errorResponse = new ErrorResponse(
                status.value(),
                error,
                message != null ? message : "Request processing failed",
                request.getRequestURI()
        );
        
        return new ResponseEntity<>(errorResponse, status);
    }
}