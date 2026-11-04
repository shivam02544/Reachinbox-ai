package com.reachinbox.app.data.api;

import com.reachinbox.app.data.model.AuthResponse;
import com.reachinbox.app.data.model.DashboardData;

import retrofit2.Call;
import retrofit2.http.GET;

public interface DashboardApi {
    @GET("dashboard")
    Call<AuthResponse<DashboardData>> getDashboard();
}
