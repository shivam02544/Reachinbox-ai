package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;

public class CreateCampaignRequest {
    @SerializedName("name")
    private String name;
    @SerializedName("senderAccountId")
    private String senderAccountId;
    @SerializedName("subject")
    private String subject;
    @SerializedName("bodyText")
    private String bodyText;
    @SerializedName("bodyHtml")
    private String bodyHtml;

    public CreateCampaignRequest(String name, String senderAccountId, String subject, String bodyText) {
        this.name = name;
        this.senderAccountId = senderAccountId;
        this.subject = subject;
        this.bodyText = bodyText;
    }

    // Getters and setters if needed
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSenderAccountId() { return senderAccountId; }
    public void setSenderAccountId(String senderAccountId) { this.senderAccountId = senderAccountId; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getBodyText() { return bodyText; }
    public void setBodyText(String bodyText) { this.bodyText = bodyText; }
    public String getBodyHtml() { return bodyHtml; }
    public void setBodyHtml(String bodyHtml) { this.bodyHtml = bodyHtml; }
}
