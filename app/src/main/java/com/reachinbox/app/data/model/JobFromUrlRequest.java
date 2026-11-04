package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;

public class JobFromUrlRequest {
    @SerializedName("url")
    private String url;

    @SerializedName("recipient")
    private GenerateOutreachRequest.Recipient recipient;

    @SerializedName("instructions")
    private String instructions;

    @SerializedName("tone")
    private String tone;

    public JobFromUrlRequest(String url) {
        this.url = url;
    }

    public JobFromUrlRequest(String url, GenerateOutreachRequest.Recipient recipient, String tone) {
        this.url = url;
        this.recipient = recipient;
        this.tone = tone;
    }
}
