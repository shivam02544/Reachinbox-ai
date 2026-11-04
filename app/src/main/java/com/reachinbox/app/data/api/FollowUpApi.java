package com.reachinbox.app.data.api;

import com.reachinbox.app.data.model.FollowUpResponse;
import com.reachinbox.app.data.model.GenerateFollowUpRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface FollowUpApi {
    @POST("ai/followup/generate")
    Call<FollowUpResponse> generateFollowUp(@Body GenerateFollowUpRequest request);
}
