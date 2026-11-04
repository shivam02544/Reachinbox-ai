package com.reachinbox.app.data.api;

import com.reachinbox.app.data.model.AuthResponse;
import com.reachinbox.app.data.model.CreateSenderRequest;
import com.reachinbox.app.data.model.Sender;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface SenderApi {
    @GET("senders")
    Call<AuthResponse<List<Sender>>> getSenders();

    @POST("senders")
    Call<AuthResponse<Sender>> createSender(@Body CreateSenderRequest request);

    @POST("senders/{id}/verify")
    Call<AuthResponse<Sender>> verifySender(@Path("id") String id);

    @DELETE("senders/{id}")
    Call<AuthResponse<Void>> deleteSender(@Path("id") String id);
}
