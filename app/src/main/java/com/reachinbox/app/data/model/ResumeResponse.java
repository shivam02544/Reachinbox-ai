package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;

public class ResumeResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("data")
    private Resume data;

    @SerializedName("message")
    private String message;

    public boolean isSuccess() { return success; }
    public Resume getData() { return data; }
    public String getMessage() { return message; }
}
