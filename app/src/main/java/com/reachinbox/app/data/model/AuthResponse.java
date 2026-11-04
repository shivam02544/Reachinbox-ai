package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;

public class AuthResponse<T> {
    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private T data;

    @SerializedName("error")
    private ErrorDetails error;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public T getData() { return data; }
    public ErrorDetails getError() { return error; }

    public static class ErrorDetails {
        @SerializedName("code")
        private String code;

        public String getCode() { return code; }
    }
}