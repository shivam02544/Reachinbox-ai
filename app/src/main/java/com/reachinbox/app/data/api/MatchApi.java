package com.reachinbox.app.data.api;

import com.reachinbox.app.data.model.GenerateMatchRequest;
import com.reachinbox.app.data.model.MatchResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface MatchApi {
    @POST("ai/match")
    Call<MatchResponse> generateMatch(@Body GenerateMatchRequest request);
}
