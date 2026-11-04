package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;

public class JobFromUrlResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private JobFromUrlResult data;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public JobFromUrlResult getData() { return data; }
}
