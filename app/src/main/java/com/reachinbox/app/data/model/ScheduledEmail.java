package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.Date;

public class ScheduledEmail {
    @SerializedName("id")
    private String id;

    @SerializedName("emailId")
    private String emailId;

    @SerializedName("scheduledAt")
    private Date scheduledAt;

    @SerializedName("status")
    private String status;

    @SerializedName("createdAt")
    private Date createdAt;

    @SerializedName("email")
    private Email email;

    public String getId() { return id; }
    public Date getScheduledAt() { return scheduledAt; }
    public String getStatus() { return status; }
    public Email getEmail() { return email; }
}
