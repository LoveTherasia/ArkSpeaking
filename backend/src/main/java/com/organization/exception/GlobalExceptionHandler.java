package com.organization.exception;


import com.organization.pojo.Result;
import org.springframework.util.StringUtils;
import com.organization.utils.DebugLogger;
import org.springframework.web.bind.annotation.ExceptionHandler;
// import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        DebugLogger.logException("GlobalException", e);
        return Result.error(StringUtils.hasLength(e.getMessage()) ? e.getMessage() : "操作失败，请稍后重试");
    }
}
