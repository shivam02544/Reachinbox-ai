package com.reachinbox.app.data.api;

import com.reachinbox.app.data.model.GenerateOutreachRequest;
import com.reachinbox.app.data.model.OutreachResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface OutreachApi {
    @POST("ai/outreach/generate")
    Call<OutreachResponse> generateOutreach(@Body GenerateOutreachRequest request);
}
