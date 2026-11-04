package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;

public class CreateSenderRequest {
    @SerializedName("displayName")
    private String displayName;

    @SerializedName("email")
    private String email;

    @SerializedName("provider")
    private String provider;

    @SerializedName("smtpHost")
    private String smtpHost;

    @SerializedName("smtpPort")
    private int smtpPort;

    @SerializedName("smtpUsername")
    private String smtpUsername;

    @SerializedName("smtpPassword")
    private String smtpPassword;

    public CreateSenderRequest(String displayName, String email, String smtpHost, int smtpPort, String smtpUsername, String smtpPassword) {
        this.displayName = displayName;
        this.email = email;
        this.provider = "SMTP";
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.smtpUsername = smtpUsername;
        this.smtpPassword = smtpPassword;
    }
}
