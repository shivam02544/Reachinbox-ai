package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;

public class InboundEmail {
    @SerializedName("id")
    private String id;

    @SerializedName("messageId")
    private String messageId;

    @SerializedName("fromEmail")
    private String fromEmail;

    @SerializedName("toEmail")
    private String toEmail;

    @SerializedName("subject")
    private String subject;

    @SerializedName("textBody")
    private String textBody;

    @SerializedName("htmlBody")
    private String htmlBody;

    @SerializedName("analysis")
    private ReplyAnalysis analysis;

    @SerializedName("createdAt")
    private String createdAt;

    public String getId() { return id; }
    public String getMessageId() { return messageId; }
    public String getFromEmail() { return fromEmail; }
    public String getToEmail() { return toEmail; }
    public String getSubject() { return subject; }
    public String getTextBody() { return textBody; }
    public String getHtmlBody() { return htmlBody; }
    public ReplyAnalysis getAnalysis() { return analysis; }
    public String getCreatedAt() { return createdAt; }
}
