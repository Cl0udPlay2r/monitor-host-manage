package com.example.controller.exception;

/**
 * 时序库（InfluxDB）查询失败异常。
 * <p>
 * 用于替代原先「catch 后 return null」的写法——null 会被 Controller 包成
 * 「成功 + 空数据」，前端只看到一张空图表且没有任何错误提示，
 * 等同于用空数据掩盖存储层故障（见测试记录 F12）。
 */
public class TimeSeriesQueryException extends RuntimeException {

    public TimeSeriesQueryException(String message, Throwable cause) {
        super(message, cause);
    }
}
