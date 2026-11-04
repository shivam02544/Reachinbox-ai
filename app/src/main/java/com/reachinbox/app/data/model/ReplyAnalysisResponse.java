package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;

public class ReplyAnalysisResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("data")
    private ReplyAnalysis data;

    public boolean isSuccess() { return success; }
    public ReplyAnalysis getData() { return data; }
}
