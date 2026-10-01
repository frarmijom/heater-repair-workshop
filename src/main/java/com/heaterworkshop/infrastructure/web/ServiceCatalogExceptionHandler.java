package com.heaterworkshop.infrastructure.web;

import com.heaterworkshop.domain.inventory.CatalogConflictException;
import com.heaterworkshop.domain.inventory.CatalogNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.Map;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = ServiceCatalogController.class)
public class ServiceCatalogExceptionHandler {

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> invalidId(
            Exception ex,
            HttpServletRequest request) {
        return error(400, "Identificador de servicio inválido.", request);
    }

    @ExceptionHandler(CatalogNotFoundException.class)
    ResponseEntity<ApiError> notFound(
            CatalogNotFoundException ex,
            HttpServletRequest request) {
        return error(404, ex.getMessage(), request);
    }

    @ExceptionHandler({
            CatalogConflictException.class,
            OptimisticLockingFailureException.class
    })
    ResponseEntity<ApiError> conflict(
            Exception ex,
            HttpServletRequest request) {
        return error(409, ex.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> duplicate(
            Exception ex,
            HttpServletRequest request) {
        return error(
                409,
                "El código de servicio ya existe.",
                request);
    }

    private ResponseEntity<ApiError> error(
            int status,
            String message,
            HttpServletRequest request) {

        return ResponseEntity.status(status)
                .body(new ApiError(
                        Instant.now(),
                        status,
                        HttpStatus.valueOf(status).getReasonPhrase(),
                        message,
                        request.getRequestURI(),
                        Map.of()));
    }
}
