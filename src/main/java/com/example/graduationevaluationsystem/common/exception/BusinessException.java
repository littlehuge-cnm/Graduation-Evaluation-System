package com.example.graduationevaluationsystem.common.exception;

/**
 * 业务异常，用于返回用户可理解的错误提示
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
