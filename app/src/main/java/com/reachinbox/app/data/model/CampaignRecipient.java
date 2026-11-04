package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.Date;

public class CampaignRecipient {
    @SerializedName("id")
    private String id;
    @SerializedName("campaignId")
    private String campaignId;
    @SerializedName("recipient")
    private String recipient;
    @SerializedName("status")
    private String status;
    @SerializedName("attempts")
    private int attempts;
    @SerializedName("sentAt")
    private Date sentAt;
    @SerializedName("failedAt")
    private Date failedAt;
    @SerializedName("errorMessage")
    private String errorMessage;
    @SerializedName("createdAt")
    private Date createdAt;
    @SerializedName("updatedAt")
    private Date updatedAt;

    public String getId() { return id; }
    public String getCampaignId() { return campaignId; }
    public String getRecipient() { return recipient; }
    public String getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public Date getSentAt() { return sentAt; }
    public Date getFailedAt() { return failedAt; }
    public String getErrorMessage() { return errorMessage; }
    public Date getCreatedAt() { return createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
}
