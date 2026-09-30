package com.heaterworkshop.infrastructure.web;
import com.heaterworkshop.domain.inventory.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.Map;

@Order(0)
@RestControllerAdvice(assignableTypes=InventoryCatalogController.class)
public class InventoryCatalogExceptionHandler {
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> invalidId(Exception ex, HttpServletRequest request) { return error(400,"Identificador de catálogo inválido.",request); }
    @ExceptionHandler(CatalogNotFoundException.class)
    ResponseEntity<ApiError> missing(CatalogNotFoundException ex, HttpServletRequest request) { return error(404,ex.getMessage(),request); }
    @ExceptionHandler({CatalogConflictException.class, OptimisticLockingFailureException.class})
    ResponseEntity<ApiError> conflict(Exception ex, HttpServletRequest request) { return error(409,"El registro cambió. Recarga antes de editar.",request); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> duplicate(Exception ex, HttpServletRequest request) { return error(409,"El nombre o símbolo ya existe en el catálogo, incluso entre registros inactivos.",request); }
    private ResponseEntity<ApiError> error(int status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ApiError(Instant.now(),status,HttpStatus.valueOf(status).getReasonPhrase(),message,request.getRequestURI(),Map.of()));
    }
}
