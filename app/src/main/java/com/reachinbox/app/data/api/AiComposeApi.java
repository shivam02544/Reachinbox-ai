package com.reachinbox.app.data.api;

import com.reachinbox.app.data.model.ImproveEmailRequest;
import com.reachinbox.app.data.model.ImproveEmailResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AiComposeApi {
    @POST("ai/compose/improve")
    Call<ImproveEmailResponse> improveEmail(@Body ImproveEmailRequest request);
}
