package com.agito.anime.common;

/**
 * 统一错误码定义（需求 5.5.5 节）
 * 约定：0 成功；1xxx 参数/校验类；2xxx 认证/权限类；3xxx 业务类；5xxx 系统/网络类
 */
public final class ErrorCode {

    public static final int OK = 0;

    // 1xxx 参数校验
    public static final int PARAM_INVALID = 1001;
    public static final int PARAM_MISSING = 1002;

    // 2xxx 认证与权限
    public static final int NOT_LOGIN = 2001;
    public static final int TOKEN_EXPIRED = 2002;
    public static final int NO_PERMISSION = 2003;
    public static final int ACCOUNT_LOCKED = 2004;
    public static final int USERNAME_EXISTS = 2005;
    public static final int LOGIN_FAILED = 2006;

    // 3xxx 业务
    public static final int DATA_NOT_FOUND = 3001;
    public static final int DATA_ALREADY_EXISTS = 3002;
    public static final int LINK_INVALID = 3003;
    public static final int REVIEW_PENDING = 3004;

    // 5xxx 系统
    public static final int SYSTEM_ERROR = 5000;

    private ErrorCode() {}
}
