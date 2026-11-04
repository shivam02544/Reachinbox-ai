package com.reachinbox.app.data.api;

import com.reachinbox.app.data.model.AuthResponse;
import com.reachinbox.app.data.model.CreateScheduledEmailRequest;
import com.reachinbox.app.data.model.ScheduledEmail;
import com.reachinbox.app.data.model.ScheduledEmailListResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ScheduledEmailApi {
    @POST("scheduled-emails")
    Call<AuthResponse<ScheduledEmail>> scheduleEmail(@Body CreateScheduledEmailRequest request);

    @GET("scheduled-emails")
    Call<AuthResponse<ScheduledEmailListResponse>> getScheduledEmails(@Query("page") int page, @Query("limit") int limit);

    @GET("scheduled-emails/{id}")
    Call<AuthResponse<ScheduledEmail>> getScheduledEmailDetails(@Path("id") String id);

    @DELETE("scheduled-emails/{id}")
    Call<AuthResponse<Void>> cancelScheduledEmail(@Path("id") String id);
}
