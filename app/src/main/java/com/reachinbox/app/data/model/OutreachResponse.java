package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;

public class OutreachResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("data")
    private OutreachGenerationResult data;

    @SerializedName("message")
    private String message;

    public boolean isSuccess() { return success; }
    public OutreachGenerationResult getData() { return data; }
    public String getMessage() { return message; }
}
