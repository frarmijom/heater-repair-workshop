package com.heaterworkshop.infrastructure.web;

import com.heaterworkshop.domain.inventory.CatalogConflictException;
import com.heaterworkshop.domain.inventory.CatalogNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.Map;

@Order(0)
@RestControllerAdvice(assignableTypes = InventoryItemController.class)
public class InventoryItemExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryItemExceptionHandler.class);
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> invalidId(Exception ex, HttpServletRequest request) {
        return error(400, "Identificador de artículo inválido.", request);
    }

    @ExceptionHandler(CatalogNotFoundException.class)
    ResponseEntity<ApiError> notFound(Exception ex, HttpServletRequest request) {
        return error(404, "No se encontró el artículo o catálogo solicitado.", request);
    }

    @ExceptionHandler({CatalogConflictException.class, ObjectOptimisticLockingFailureException.class})
    ResponseEntity<ApiError> conflict(Exception ex, HttpServletRequest request) {
        return error(409, ex.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> duplicate(Exception ex, HttpServletRequest request) {
        return error(409, "SKU o requestId ya utilizado; revisa el artículo existente.", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> invalid(Exception ex, HttpServletRequest request) {
        return error(400, ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception ex, HttpServletRequest request) {
        LOGGER.error("Unexpected inventory item request failure", ex);
        return error(500, "No se pudo completar la operación de inventario.", request);
    }

    private ResponseEntity<ApiError> error(int status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ApiError(Instant.now(), status,
                HttpStatus.valueOf(status).getReasonPhrase(), message, request.getRequestURI(), Map.of()));
    }
}