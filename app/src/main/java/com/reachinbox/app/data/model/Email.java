package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.Date;

public class Email {
    @SerializedName("id")
    private String id;

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

    @SerializedName("status")
    private String status;

    @SerializedName("createdAt")
    private Date createdAt;

    @SerializedName("senderAccount")
    private Sender senderAccount;

    @SerializedName("scheduledEmails")
    private java.util.List<ScheduledEmail> scheduledEmails;

    @SerializedName("parentEmailId")
    private String parentEmailId;

    @SerializedName("followUpNumber")
    private Integer followUpNumber;

    public String getId() { return id; }
    public String getRecipient() { return recipient; }
    public String getSubject() { return subject; }
    public String getHtmlBody() { return htmlBody; }
    public String getTextBody() { return textBody; }
    public String getStatus() { return status; }
    public Date getCreatedAt() { return createdAt; }
    public Sender getSenderAccount() { return senderAccount; }
    public java.util.List<ScheduledEmail> getScheduledEmails() { return scheduledEmails; }
    public String getParentEmailId() { return parentEmailId; }
    public Integer getFollowUpNumber() { return followUpNumber; }
}
