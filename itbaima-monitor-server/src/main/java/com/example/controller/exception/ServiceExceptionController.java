package com.example.controller.exception;

import com.example.entity.RestBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 业务层异常处理：把明确的服务不可用错误转换成带可读信息的失败响应，
 * 避免被默认的 500「内部错误，请联系管理员」掩盖掉真正的故障原因。
 */
@Slf4j
@RestControllerAdvice
public class ServiceExceptionController {

    @ExceptionHandler(TimeSeriesQueryException.class)
    public RestBean<Void> queryError(TimeSeriesQueryException exception) {
        log.warn("时序库查询失败: {}", exception.getMessage());
        return RestBean.failure(500, exception.getMessage());
    }
}
