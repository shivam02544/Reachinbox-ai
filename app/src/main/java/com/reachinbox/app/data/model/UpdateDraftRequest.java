package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;

public class UpdateDraftRequest {
    @SerializedName("senderAccountId")
    private String senderAccountId;

    @SerializedName("recipient")
    private String recipient;

    @SerializedName("subject")
    private String subject;

    @SerializedName("htmlBody")
    private String htmlBody;

    @SerializedName("textBody")
    private String textBody;

    @SerializedName("parentEmailId")
    private String parentEmailId;

    @SerializedName("followUpNumber")
    private Integer followUpNumber;

    public void setSenderAccountId(String senderAccountId) { this.senderAccountId = senderAccountId; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    public void setSubject(String subject) { this.subject = subject; }
    public void setHtmlBody(String htmlBody) { this.htmlBody = htmlBody; }
    public void setTextBody(String textBody) { this.textBody = textBody; }
    public void setParentEmailId(String parentEmailId) { this.parentEmailId = parentEmailId; }
    public void setFollowUpNumber(Integer followUpNumber) { this.followUpNumber = followUpNumber; }
}
