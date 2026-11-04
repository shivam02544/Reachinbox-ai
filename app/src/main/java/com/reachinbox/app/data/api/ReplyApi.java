package com.reachinbox.app.data.api;

import com.reachinbox.app.data.model.ReplyAnalysisResponse;

import retrofit2.Call;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ReplyApi {
    @POST("ai/reply/{inboundEmailId}/analyze")
    Call<ReplyAnalysisResponse> analyzeReply(@Path("inboundEmailId") String inboundEmailId);
}
