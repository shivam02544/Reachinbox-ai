package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;

public class CreateEmailRequest {
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

    @SerializedName("idempotencyKey")
    private String idempotencyKey;

    @SerializedName("parentEmailId")
    private String parentEmailId;

    @SerializedName("followUpNumber")
    private Integer followUpNumber;

    public CreateEmailRequest(String senderAccountId, String recipient, String subject, String body, boolean isHtml, String idempotencyKey) {
        this.senderAccountId = senderAccountId;
        this.recipient = recipient;
        this.subject = subject;
        if (isHtml) {
            this.htmlBody = body;
        } else {
            this.textBody = body;
        }
        this.idempotencyKey = idempotencyKey;
    }

    public void setParentEmailId(String parentEmailId) {
        this.parentEmailId = parentEmailId;
    }

    public void setFollowUpNumber(Integer followUpNumber) {
        this.followUpNumber = followUpNumber;
    }
}
