package com.agito.anime.common;

import lombok.Getter;

/**
 * 业务异常（携带错误码，由全局异常处理器统一翻译为出参）
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public static BizException param(String message) {
        return new BizException(ErrorCode.PARAM_INVALID, message);
    }

    public static BizException notFound(String message) {
        return new BizException(ErrorCode.DATA_NOT_FOUND, message);
    }

    public static BizException linkInvalid(String message) {
        return new BizException(ErrorCode.LINK_INVALID, message);
    }
}
