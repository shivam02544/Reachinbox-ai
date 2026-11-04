package com.reachinbox.app.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.reachinbox.app.data.api.ApiClient;
import com.reachinbox.app.data.api.AuthApi;
import com.reachinbox.app.data.model.AuthData;
import com.reachinbox.app.data.model.AuthResponse;
import com.reachinbox.app.data.model.LoginRequest;
import com.reachinbox.app.data.model.LogoutRequest;
import com.reachinbox.app.data.model.RegisterRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {
    private final AuthApi authApi;

    public AuthRepository(Context context) {
        this.authApi = ApiClient.getClient(context).create(AuthApi.class);
    }

    public interface AuthCallback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    public void register(String name, String email, String password, AuthCallback<AuthData> callback) {
        RegisterRequest request = new RegisterRequest(name, email, password);
        authApi.register(request).enqueue(new Callback<AuthResponse<AuthData>>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse<AuthData>> call, @NonNull Response<AuthResponse<AuthData>> response) {
                handleAuthResponse(response, callback);
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse<AuthData>> call, @NonNull Throwable t) {
                if (t instanceof java.net.SocketTimeoutException) {
                    callback.onError("Server connection timed out. Please try again.");
                } else if (t instanceof java.io.IOException) {
                    callback.onError("Unable to connect to the server. Check your network connection.");
                } else {
                    callback.onError("Network error: " + t.getMessage());
                }
            }
        });
    }

    public void login(String email, String password, AuthCallback<AuthData> callback) {
        LoginRequest request = new LoginRequest(email, password);
        authApi.login(request).enqueue(new Callback<AuthResponse<AuthData>>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse<AuthData>> call, @NonNull Response<AuthResponse<AuthData>> response) {
                handleAuthResponse(response, callback);
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse<AuthData>> call, @NonNull Throwable t) {
                if (t instanceof java.net.SocketTimeoutException) {
                    callback.onError("Server connection timed out. Please try again.");
                } else if (t instanceof java.io.IOException) {
                    callback.onError("Unable to connect to the server. Check your network connection.");
                } else {
                    callback.onError("Network error: " + t.getMessage());
                }
            }
        });
    }

    public void logout(String refreshToken, AuthCallback<Void> callback) {
        LogoutRequest request = new LogoutRequest(refreshToken);
        authApi.logout(request).enqueue(new Callback<AuthResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse<Void>> call, @NonNull Response<AuthResponse<Void>> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(null);
                } else {
                    callback.onError("Logout failed: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse<Void>> call, @NonNull Throwable t) {
                // Still notify success in callback because local session should be cleared anyway
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void getSession(AuthCallback<AuthData> callback) {
        authApi.getSession().enqueue(new Callback<AuthResponse<AuthData>>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse<AuthData>> call, @NonNull Response<AuthResponse<AuthData>> response) {
                handleAuthResponse(response, callback);
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse<AuthData>> call, @NonNull Throwable t) {
                if (t instanceof java.net.SocketTimeoutException) {
                    callback.onError("Server connection timed out. Please try again.");
                } else if (t instanceof java.io.IOException) {
                    callback.onError("Unable to connect to the server. Check your network connection.");
                } else {
                    callback.onError("Network error: " + t.getMessage());
                }
            }
        });
    }

    private void handleAuthResponse(Response<AuthResponse<AuthData>> response, AuthCallback<AuthData> callback) {
        if (response.isSuccessful() && response.body() != null) {
            if (response.body().isSuccess()) {
                callback.onSuccess(response.body().getData());
            } else {
                String msg = response.body().getMessage();
                callback.onError(msg != null && !msg.trim().isEmpty() ? msg : "Authentication failed");
            }
        } else {
            String errorMsg = null;
            if (response.errorBody() != null) {
                try {
                    String rawJson = response.errorBody().string();
                    if (rawJson != null && !rawJson.trim().isEmpty()) {
                        com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseString(rawJson).getAsJsonObject();
                        if (jsonObject.has("message") && !jsonObject.get("message").isJsonNull()) {
                            errorMsg = jsonObject.get("message").getAsString();
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            
            if (errorMsg != null && !errorMsg.trim().isEmpty()) {
                callback.onError(errorMsg);
            } else if (response.code() == 401) {
                callback.onError("Invalid email or password");
            } else if (response.code() == 400) {
                callback.onError("Bad request parameters. Please verify input fields.");
            } else if (response.code() >= 500) {
                callback.onError("Something went wrong on the server. Please try again.");
            } else {
                callback.onError("Something went wrong. Please try again.");
            }
        }
    }
}