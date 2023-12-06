package com.matejamusa.InvoiceFlow.exception;

import com.matejamusa.InvoiceFlow.model.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.nio.file.AccessDeniedException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.time.LocalDateTime.now;
import static org.springframework.http.HttpStatus.*;

@RestControllerAdvice
@Slf4j
public class HandleException extends ResponseEntityExceptionHandler implements ErrorController {
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        return new ResponseEntity<>(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .reason(ex.getMessage())
                        .developerMessage(ex.getMessage())
                        .status(HttpStatus.resolve(statusCode.value()))
                        .statusCode(statusCode.value())
                        .build(), statusCode);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
        String fieldMessage = fieldErrors.stream().map(FieldError::getDefaultMessage).collect(Collectors.joining(", "));
        return new ResponseEntity<>(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .reason(fieldMessage)
                        .developerMessage(ex.getMessage())
                        .status(HttpStatus.resolve(statusCode.value()))
                        .statusCode(statusCode.value())
                        .build(), statusCode);
    }

    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public ResponseEntity<HttpResponse> sQLIntegrityConstraintViolationException(SQLIntegrityConstraintViolationException ex) {
        log.error(ex.getMessage());
        return new ResponseEntity<>(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .reason(ex.getMessage().contains("Duplicate entry") ? "Information already exists" : ex.getMessage())
                        .developerMessage(ex.getMessage())
                        .status(BAD_REQUEST)
                        .statusCode(BAD_REQUEST.value())
                        .build(), BAD_REQUEST);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<HttpResponse> badCredentialsException(BadCredentialsException ex) {
        log.error(ex.getMessage());
        return new ResponseEntity<>(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .reason(ex.getMessage() + ", Incorrect email or password")
                        .developerMessage(ex.getMessage())
                        .status(BAD_REQUEST)
                        .statusCode(BAD_REQUEST.value())
                        .build(), BAD_REQUEST);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<HttpResponse> apiException(ApiException ex) {
        log.error(ex.getMessage());
        return new ResponseEntity<>(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .reason(ex.getMessage())
                        .developerMessage(ex.getMessage())
                        .status(FORBIDDEN)
                        .statusCode(FORBIDDEN.value())
                        .build(), FORBIDDEN);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<HttpResponse> accessDeniedException(AccessDeniedException ex) {
        log.error(ex.getMessage());
        return new ResponseEntity<>(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .reason("Access denied. You don\'t have access.")
                        .developerMessage(ex.getMessage())
                        .status(BAD_REQUEST)
                        .statusCode(BAD_REQUEST.value())
                        .build(), BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<HttpResponse> exception(Exception ex) {
        log.error(ex.getMessage());
        return new ResponseEntity<>(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .reason(ex.getMessage() != null ?
                                (ex.getMessage().contains("expected 1, actual 0") ? "Record not found" : ex.getMessage())
                                : "Some error occurred.")
                        .developerMessage(ex.getMessage())
                        .status(INTERNAL_SERVER_ERROR)
                        .statusCode(INTERNAL_SERVER_ERROR.value())
                        .build(), INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    public ResponseEntity<HttpResponse> emptyResultDataAccessException(EmptyResultDataAccessException ex) {
        log.error(ex.getMessage());
        return new ResponseEntity<>(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .reason(ex.getMessage().contains("expected 1, actual 0") ? "Record not found" : ex.getMessage())
                        .developerMessage(ex.getMessage())
                        .status(BAD_REQUEST)
                        .statusCode(BAD_REQUEST.value())
                        .build(), BAD_REQUEST);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<HttpResponse> disabledException(DisabledException ex) {
        log.error(ex.getMessage());
        return new ResponseEntity<>(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .reason("User account is already disabled")
                        .developerMessage(ex.getMessage())
                        .status(BAD_REQUEST)
                        .statusCode(BAD_REQUEST.value())
                        .build(), BAD_REQUEST);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<HttpResponse> lockedException(LockedException exception) {
        log.error(exception.getMessage());
        return new ResponseEntity<>(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .developerMessage(exception.getMessage())
                        //.reason(exception.getMessage() + ", too many failed attempts.")
                        .reason("User account is currently locked")
                        .status(BAD_REQUEST)
                        .statusCode(BAD_REQUEST.value()).build()
                , BAD_REQUEST);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<HttpResponse> dataAccessException(DataAccessException exception) {
        log.error(exception.getMessage());
        return new ResponseEntity<>(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .reason(processErrorMessage(exception.getMessage()))
                        .developerMessage(processErrorMessage(exception.getMessage()))
                        .status(BAD_REQUEST)
                        .statusCode(BAD_REQUEST.value()).build()
                , BAD_REQUEST);
    }

    private ResponseEntity<HttpResponse> createErrorHttpResponse(HttpStatus httpStatus, String reason, Exception exception) {
        return new ResponseEntity<>(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .developerMessage(exception.getMessage())
                        .reason(reason)
                        .status(httpStatus)
                        .statusCode(httpStatus.value()).build()
                , httpStatus);
    }

    private String processErrorMessage(String errorMessage) {
        if(errorMessage != null) {
            if(errorMessage.contains("Duplicate entry") && errorMessage.contains("AccountVerifications")) {
                return "You already verified your account.";
            }
            if(errorMessage.contains("Duplicate entry") && errorMessage.contains("ResetPasswordVerifications")) {
                return "We already sent you an email to reset your password.";
            }
            if(errorMessage.contains("Duplicate entry")) {
                return "Duplicate entry. Please try again.";
            }
        }
        return "Some error occurred";
    }
}
