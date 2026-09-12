package com.zwx.codepulse.common;

import com.zwx.codepulse.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;

/**
 * @author: 张伟旭
 * @Create: 2026-06-01 17:35
 * @description:
 **/

@Data
public class BaseResponse<T> implements Serializable {

    private int code;
    private T data;
    private String message;

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}

