package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;

public class ImproveEmailRequest {
    @SerializedName("subject") private final String subject;
    @SerializedName("body") private final String body;
    @SerializedName("tone") private final String tone;

    public ImproveEmailRequest(String subject, String body, String tone) {
        this.subject = subject;
        this.body = body;
        this.tone = tone;
    }
}
