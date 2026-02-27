package dtu.services.library.errors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import static net.logstash.logback.argument.StructuredArguments.kv;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;


@RestControllerAdvice
class ErrorHandler
{
    private static final Logger log = LoggerFactory.getLogger(ErrorHandler.class);


    // Handle errors propagated from other services
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ErrorResponse> handleServiceException(ServiceException ex, HttpServletRequest request)
    {
        ErrorResponse error = new ErrorResponse
        (
            ex.getStatus(),
            ex.getMessage()
        );

        log.error("Downstream service error",
              kv("status", error.getStatus()),
              kv("path", request.getRequestURI()),
              kv("message", error.getMessage()));

        return(new ResponseEntity<>(error, error.getHttpStatus()));
    }


    // Handle any resource not found (Internal or Spring-thrown)
    @ExceptionHandler({ResourceNotFoundException.class, NoResourceFoundException.class,NoHandlerFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(Exception ex, HttpServletRequest request)
    {
        ErrorResponse error = new ErrorResponse
        (
            HttpStatus.NOT_FOUND,
            ex.getMessage()
        );

        log.error("Resource not found",
              kv("status", error.getStatus()),
              kv("path", request.getRequestURI()),
              kv("message", error.getMessage()));

        return(new ResponseEntity<>(error, error.getHttpStatus()));
    }


    // Handle invalid HTTP verbs (e.g., POST instead of GET)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest request)
    {
        ErrorResponse error = new ErrorResponse
        (
            HttpStatus.METHOD_NOT_ALLOWED,
            ex.getMessage()
        );

        log.error("Method not allowed",
              kv("status", error.getStatus()),
              kv("path", request.getRequestURI()),
              kv("message", error.getMessage()));

        return(new ResponseEntity<>(error, error.getHttpStatus()));
    }


    // Handle malformed JSON or type mismatches in request parameters
    @ExceptionHandler({HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex, HttpServletRequest request)
    {
        ErrorResponse error = new ErrorResponse
        (
            HttpStatus.BAD_REQUEST,
            "Invalid request format or parameter type"
        );

        log.error("Bad request",
              kv("status", error.getStatus()),
              kv("path", request.getRequestURI()),
              kv("message", ex.getMessage()));

        return(new ResponseEntity<>(error, error.getHttpStatus()));
    }


    // Handle validation errors (Binding errors)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request)
    {
        String details = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining(", "));

        ErrorResponse error = new ErrorResponse
        (
            HttpStatus.BAD_REQUEST,
            details
        );

        log.error("Invalid payload",
              kv("status", error.getStatus()),
              kv("path", request.getRequestURI()),
              kv("message", error.getMessage()));

        return(new ResponseEntity<>(error, error.getHttpStatus()));
    }


    // Handle unexpected
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, HttpServletRequest request)
    {
        ErrorResponse error = new ErrorResponse
        (
            HttpStatus.INTERNAL_SERVER_ERROR,
            ex.getMessage()
        );

        log.error("An unexpected error occurred",
              kv("status", error.getStatus()),
              kv("path", request.getRequestURI()),
              kv("message", error.getMessage()));

        return(new ResponseEntity<>(error, error.getHttpStatus()));
    }
}