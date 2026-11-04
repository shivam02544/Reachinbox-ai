package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;

public class AuthData {
    @SerializedName("user")
    private User user;

    @SerializedName("accessToken")
    private String accessToken;

    @SerializedName("refreshToken")
    private String refreshToken;

    @SerializedName("expiresIn")
    private Integer expiresIn;

    public User getUser() { return user; }
    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public Integer getExpiresIn() { return expiresIn; }
}