package com.nordigy.testrestapi.api;

import com.nordigy.testrestapi.model.error.ErrorResponse;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.rest.core.RepositoryConstraintViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.MimeType;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.persistence.EntityNotFoundException;
import java.util.stream.Collectors;

import static com.nordigy.testrestapi.model.error.ErrorResponse.builder;
import static java.lang.String.format;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE;

@ControllerAdvice
public class ExceptionHandlerController extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ExceptionHandlerController.class);

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, HttpHeaders headers,
            HttpStatus status, WebRequest request) {
        return buildResponseEntity(builder()
                .status(BAD_REQUEST)
                .message(format("Parameter is missing: %s", ex.getParameterName()))
                .debugMessage(ex.getLocalizedMessage())
                .build());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatus status, WebRequest request) {
        BindingResult bindingResult = ex.getBindingResult();
        return buildResponseEntity(
                builder()
                        .status(BAD_REQUEST)
                        .message("Validation error")
                        .subErrors(bindingResult.getFieldErrors())
                        .subErrors(bindingResult.getGlobalErrors())
                        .build());
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotWritable(
            HttpMessageNotWritableException ex, HttpHeaders headers,
            HttpStatus status, WebRequest request) {
        return buildResponseEntity(builder()
                .status(INTERNAL_SERVER_ERROR)
                .message("Failed to write JSON output")
                .debugMessage(ex.getLocalizedMessage())
                .build());
    }

    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(
            NoHandlerFoundException ex, HttpHeaders headers,
            HttpStatus status, WebRequest request) {
        return buildResponseEntity(builder()
                .status(BAD_REQUEST)
                .message(format("Unsupported method %s with URL %s", ex.getHttpMethod(), ex.getRequestURL()))
                .debugMessage(ex.getMessage())
                .build());
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, HttpHeaders headers,
            HttpStatus status, WebRequest request) {
        String supportedTypes = ex.getSupportedMediaTypes().stream().map(MimeType::toString).collect(Collectors.joining(", "));
        String message = format("%s media type is not supported. Supported media types: %s", ex.getContentType(), supportedTypes);
        return buildResponseEntity(builder()
                .status(UNSUPPORTED_MEDIA_TYPE)
                .message(message)
                .build());
    }

    @ExceptionHandler(javax.validation.ConstraintViolationException.class)
    protected ResponseEntity<Object> handleConstraintViolation(
            javax.validation.ConstraintViolationException ex) {
        return buildResponseEntity(
                builder()
                        .status(BAD_REQUEST)
                        .message("Validation error")
                        .subErrors(ex.getConstraintViolations())
                        .build());
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers,
            HttpStatus status, WebRequest request) {
        return buildResponseEntity(builder()
                .status(BAD_REQUEST)
                .message("Wrong content-type of the request format. Expected content-type is application/json.")
                .debugMessage(ex.getLocalizedMessage())
                .build()
        );
    }

    @ExceptionHandler(NumberFormatException.class)
    protected ResponseEntity<Object> handleEntityNotFound(
            NumberFormatException ex) {
        return buildResponseEntity(builder()
                .status(BAD_REQUEST)
                .message(ex.getMessage())
                .build());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    protected ResponseEntity<Object> handleEntityNotFound(
            EntityNotFoundException ex) {
        return buildResponseEntity(builder()
                .status(NOT_FOUND)
                .message(ex.getMessage())
                .build());
    }

    @ExceptionHandler(RepositoryConstraintViolationException.class)
    protected ResponseEntity<Object> handleEntityNotFound(
            RepositoryConstraintViolationException ex) {
        Errors errors = ex.getErrors();
        return buildResponseEntity(builder()
                .status(BAD_REQUEST)
                .message(ex.getMessage())
                .subErrors(errors.getFieldErrors())
                .subErrors(errors.getGlobalErrors())
                .build());

    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<Object> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        return buildResponseEntity(builder()
                .status(BAD_REQUEST)
                .message(format("The parameter '%s' of value '%s' could not be converted to type '%s'", ex.getName(), ex.getValue(),
                        ex.getRequiredType().getSimpleName()))
                .debugMessage(ex.getMessage())
                .build());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    protected ResponseEntity<Object> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, WebRequest request) {
        if (ex.getCause() instanceof ConstraintViolationException) {
            return buildResponseEntity(builder()
                    .status(CONFLICT)
                    .message("Database error")
                    .debugMessage(((ConstraintViolationException) ex.getCause()).getSQLException().getLocalizedMessage())
                    .build());

        }
        return buildResponseEntity(builder()
                .status(INTERNAL_SERVER_ERROR)
                .message("Server error")
                .debugMessage(ex.getLocalizedMessage())
                .build());
    }

    private ResponseEntity<Object> buildResponseEntity(ErrorResponse errorResponse) {
        return new ResponseEntity<>(errorResponse, errorResponse.getStatus());
    }
}