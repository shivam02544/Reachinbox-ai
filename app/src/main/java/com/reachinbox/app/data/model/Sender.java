package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.Date;

public class Sender {
    @SerializedName("id")
    private String id;

    @SerializedName("displayName")
    private String displayName;

    @SerializedName("email")
    private String email;

    @SerializedName("provider")
    private String provider;

    @SerializedName("status")
    private String status;

    @SerializedName("createdAt")
    private Date createdAt;

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
    public String getProvider() { return provider; }
    public String getStatus() { return status; }
    public Date getCreatedAt() { return createdAt; }
}
