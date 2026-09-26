package io.chronicle.platform;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ExceptionAdvice {
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ApiResponse> service(ServiceException e) {
        return ResponseEntity.status(e.status()).body(new ApiResponse(e.status(), e.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse> denied() {
        return ResponseEntity.status(403).body(new ApiResponse(403, "没有访问权限"));
    }

    @ExceptionHandler({
        IllegalArgumentException.class,
        org.springframework.http.converter.HttpMessageNotReadableException.class,
        org.springframework.web.bind.MissingServletRequestParameterException.class,
        org.springframework.web.multipart.MaxUploadSizeExceededException.class
    })
    public ResponseEntity<ApiResponse> invalid() {
        return ResponseEntity.badRequest().body(new ApiResponse(400, "请求参数或文件大小不正确"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse> conflict() {
        return ResponseEntity.badRequest().body(new ApiResponse(400, "数据不完整或与已有记录冲突"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> other(Exception e) {
        org.slf4j.LoggerFactory.getLogger(getClass())
                .error("Request failed: {}", e.getClass().getSimpleName());
        return ResponseEntity.internalServerError().body(new ApiResponse(500, "服务暂时无法完成请求"));
    }
}
