package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;

public class JobRequirementResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("data")
    private JobRequirement data;

    @SerializedName("message")
    private String message;

    public boolean isSuccess() { return success; }
    public JobRequirement getData() { return data; }
    public String getMessage() { return message; }
}
