package com.reachinbox.app.data.api;

import com.reachinbox.app.data.model.AuthData;
import com.reachinbox.app.data.model.AuthResponse;
import com.reachinbox.app.data.model.LoginRequest;
import com.reachinbox.app.data.model.LogoutRequest;
import com.reachinbox.app.data.model.RefreshRequest;
import com.reachinbox.app.data.model.RegisterRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface AuthApi {
    @POST("auth/register")
    Call<AuthResponse<AuthData>> register(@Body RegisterRequest request);

    @POST("auth/login")
    Call<AuthResponse<AuthData>> login(@Body LoginRequest request);

    @POST("auth/refresh")
    Call<AuthResponse<AuthData>> refresh(@Body RefreshRequest request);

    @POST("auth/logout")
    Call<AuthResponse<Void>> logout(@Body LogoutRequest request);

    @GET("auth/session")
    Call<AuthResponse<AuthData>> getSession();
}