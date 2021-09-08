package io.buildman.common.models;

import java.io.Serializable;

public class BaseResponse<T> implements Serializable {
    public boolean success;
    public T data;

    public BaseResponse() {

    }

    public BaseResponse(boolean success, T data) {
        this.success = success;
        this.data = data;
    }
}
