package com.reachinbox.app.data.api;

import com.reachinbox.app.data.model.ActivityListResponse;
import com.reachinbox.app.data.model.AuthResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ActivityApi {
    @GET("activity")
    Call<AuthResponse<ActivityListResponse>> getActivities(@Query("page") int page, @Query("limit") int limit);
}
