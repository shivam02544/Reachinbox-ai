package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;

public class MatchResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("data")
    private MatchAnalysis data;

    @SerializedName("message")
    private String message;

    public boolean isSuccess() { return success; }
    public MatchAnalysis getData() { return data; }
    public String getMessage() { return message; }
}
