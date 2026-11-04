package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;

public class ImproveEmailResponse {
    @SerializedName("success") private boolean success;
    @SerializedName("data") private Suggestion data;
    @SerializedName("message") private String message;

    public boolean isSuccess() { return success; }
    public Suggestion getData() { return data; }
    public String getMessage() { return message; }

    public static class Suggestion {
        @SerializedName("subject") private String subject;
        @SerializedName("body") private String body;
        public String getSubject() { return subject; }
        public String getBody() { return body; }
    }
}
