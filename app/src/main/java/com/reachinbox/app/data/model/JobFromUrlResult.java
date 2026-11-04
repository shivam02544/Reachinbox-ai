package com.reachinbox.app.data.model;

import com.google.gson.annotations.SerializedName;

public class JobFromUrlResult {
    @SerializedName("sourceUrl")
    private String sourceUrl;

    @SerializedName("jobRequirement")
    private JobRequirement jobRequirement;

    @SerializedName("match")
    private MatchAnalysis match;

    @SerializedName("outreach")
    private OutreachGenerationResult outreach;

    @SerializedName("suggestedSendAt")
    private String suggestedSendAt;

    public String getSourceUrl() { return sourceUrl; }
    public JobRequirement getJobRequirement() { return jobRequirement; }
    public MatchAnalysis getMatch() { return match; }
    public OutreachGenerationResult getOutreach() { return outreach; }
    public String getSuggestedSendAt() { return suggestedSendAt; }
}
