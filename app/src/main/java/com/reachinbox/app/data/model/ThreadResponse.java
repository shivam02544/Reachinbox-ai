package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ThreadResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("data")
    private ThreadData data;

    public boolean isSuccess() { return success; }
    public ThreadData getData() { return data; }

    public static class ThreadData {
        @SerializedName("rootEmailId")
        private String rootEmailId;

        @SerializedName("outboundEmails")
        private List<Email> outboundEmails;

        @SerializedName("inboundEmails")
        private List<InboundEmail> inboundEmails;

        public String getRootEmailId() { return rootEmailId; }
        public List<Email> getOutboundEmails() { return outboundEmails; }
        public List<InboundEmail> getInboundEmails() { return inboundEmails; }
    }
}
