package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.Date;

public class RecentActivity {
    @SerializedName("id")
    private String id;

    @SerializedName("type")
    private String type;

    @SerializedName("message")
    private String message;

    @SerializedName("createdAt")
    private Date createdAt;

    @SerializedName("emailId")
    private String emailId;

    public String getId() { return id; }
    public String getType() { return type; }
    public String getMessage() { return message; }
    public Date getCreatedAt() { return createdAt; }
    public String getEmailId() { return emailId; }
}
