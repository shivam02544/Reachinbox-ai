package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.Date;
import java.util.List;

public class Campaign {
    @SerializedName("id")
    private String id;
    @SerializedName("userId")
    private String userId;
    @SerializedName("senderAccountId")
    private String senderAccountId;
    @SerializedName("name")
    private String name;
    @SerializedName("subject")
    private String subject;
    @SerializedName("bodyHtml")
    private String bodyHtml;
    @SerializedName("bodyText")
    private String bodyText;
    @SerializedName("status")
    private String status;
    @SerializedName("scheduledAt")
    private Date scheduledAt;
    @SerializedName("createdAt")
    private Date createdAt;
    @SerializedName("updatedAt")
    private Date updatedAt;

    @SerializedName("senderAccount")
    private Sender senderAccount;

    @SerializedName("recipients")
    private List<CampaignRecipient> recipients;

    @SerializedName("stats")
    private CampaignStats stats;

    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getSenderAccountId() { return senderAccountId; }
    public String getName() { return name; }
    public String getSubject() { return subject; }
    public String getBodyHtml() { return bodyHtml; }
    public String getBodyText() { return bodyText; }
    public String getStatus() { return status; }
    public Date getScheduledAt() { return scheduledAt; }
    public Date getCreatedAt() { return createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public Sender getSenderAccount() { return senderAccount; }
    public List<CampaignRecipient> getRecipients() { return recipients; }
    public CampaignStats getStats() { return stats; }
}
