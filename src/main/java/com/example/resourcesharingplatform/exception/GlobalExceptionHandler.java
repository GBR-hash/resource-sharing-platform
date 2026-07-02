package com.example.resourcesharingplatform.exception;

import com.example.resourcesharingplatform.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<?>> handleBusinessException(BusinessException e) {
        log.error("业务异常: {}", e.getMessage());
        Result<?> result = Result.error(e.getCode(), e.getMessage());
        return ResponseEntity.status(e.getCode()).body(result);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Map<String, String>>> handleValidationException(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        log.error("参数验证失败: {}", errors);
        Result<Map<String, String>> result = Result.error(400, "参数验证失败");
        result.setData(errors);
        return ResponseEntity.badRequest().body(result);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<?>> handleAccessDeniedException(AccessDeniedException e) {
        log.error("权限不足: {}", e.getMessage());
        Result<?> result = Result.forbidden("权限不足");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Result<?>> handleBadCredentialsException(BadCredentialsException e) {
        log.error("认证失败: {}", e.getMessage());
        Result<?> result = Result.unauthorized("用户名或密码错误");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<?>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.error("文件大小超过限制: {}", e.getMessage());
        Result<?> result = Result.error(400, "文件大小超过限制（最大50MB）");
        return ResponseEntity.badRequest().body(result);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<?>> handleException(Exception e) {
        log.error("系统异常: ", e);
        Result<?> result = Result.error("系统异常，请稍后重试");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }
}