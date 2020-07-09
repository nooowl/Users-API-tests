package com.nordigy.testrestapi.model.error;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.hibernate.validator.internal.engine.path.PathImpl;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import javax.validation.ConstraintViolation;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Data
public class ErrorResponse {

    private HttpStatus status;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    private LocalDateTime timestamp;
    private String message;
    private String debugMessage;
    private List<ValidationError> subErrors;

    private ErrorResponse() {
        timestamp = LocalDateTime.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private HttpStatus status;
        private String message;
        private String debugMessage;
        private List<ValidationError> subErrors;

        private Builder() {
        }

        public Builder status(HttpStatus status) {
            this.status = status;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder debugMessage(String debugMessage) {
            this.debugMessage = debugMessage;
            return this;
        }

        public Builder subErrors(Collection<?> errors) {
            if (this.subErrors == null) {
                this.subErrors = new ArrayList<>();
            }
            errors.forEach(error -> {
                ValidationError validationError;
                if (error instanceof FieldError) {
                    validationError = buildValidationError((FieldError) error);
                } else if (error instanceof ObjectError) {
                    validationError = buildValidationError((ObjectError) error);
                } else if (error instanceof ConstraintViolation) {
                    validationError = buildValidationError((ConstraintViolation<?>) error);
                } else {
                    throw new RuntimeException("Wrong error format");
                }
                subErrors.add(validationError);
            });
            return this;
        }

        private ValidationError buildValidationError(ConstraintViolation<?> constraintViolation) {
            return new ValidationError(
                    constraintViolation.getRootBeanClass().getSimpleName(),
                    ((PathImpl) constraintViolation.getPropertyPath()).getLeafNode().asString(),
                    constraintViolation.getInvalidValue(),
                    constraintViolation.getMessage());
        }

        private ValidationError buildValidationError(ObjectError error) {
            return new ValidationError(
                    error.getObjectName(),
                    error.getDefaultMessage());
        }

        private ValidationError buildValidationError(FieldError error) {
            return new ValidationError(
                    error.getObjectName(),
                    error.getField(),
                    error.getRejectedValue(),
                    error.getDefaultMessage());
        }

        public ErrorResponse build() {
            ErrorResponse errorResponse = new ErrorResponse();
            errorResponse.setStatus(this.status);
            errorResponse.setMessage(this.message);
            errorResponse.setDebugMessage(this.debugMessage);
            errorResponse.setSubErrors(this.subErrors);
            return errorResponse;
        }
    }
}