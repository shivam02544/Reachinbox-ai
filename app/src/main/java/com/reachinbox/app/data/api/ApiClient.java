package com.reachinbox.app.data.api;

import android.content.Context;

import androidx.annotation.NonNull;

import com.reachinbox.app.BuildConfig;
import com.reachinbox.app.data.local.SessionManager;
import com.reachinbox.app.data.model.AuthData;
import com.reachinbox.app.data.model.AuthResponse;
import com.reachinbox.app.data.model.RefreshRequest;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Authenticator;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String BASE_URL = BuildConfig.BASE_URL;
    private static Retrofit retrofit = null;

    public static Retrofit getClient(Context context) {
        if (retrofit == null) {
            SessionManager sessionManager = new SessionManager(context.getApplicationContext());

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(BuildConfig.DEBUG
                    ? HttpLoggingInterceptor.Level.BASIC
                    : HttpLoggingInterceptor.Level.NONE);

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .addInterceptor(logging)
                    .addInterceptor(new AuthInterceptor(sessionManager))
                    .authenticator(new TokenAuthenticator(sessionManager))
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit;
    }

    /**
     * Interceptor to add Bearer token to all requests except auth endpoints
     */
    private static class AuthInterceptor implements Interceptor {
        private final SessionManager sessionManager;

        public AuthInterceptor(SessionManager sessionManager) {
            this.sessionManager = sessionManager;
        }

        @NonNull
        @Override
        public Response intercept(@NonNull Chain chain) throws IOException {
            Request originalRequest = chain.request();
            String path = originalRequest.url().encodedPath();

            // Don't add a stale session token to public authentication endpoints.
            if (path.endsWith("/auth/login") || path.endsWith("/auth/register") || 
                path.endsWith("/auth/refresh") || path.endsWith("/auth/logout")) {
                return chain.proceed(originalRequest);
            }

            String token = sessionManager.getAccessToken();
            if (token == null) {
                return chain.proceed(originalRequest);
            }

            Request newRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer " + token)
                    .build();

            return chain.proceed(newRequest);
        }
    }

    /**
     * Authenticator to handle 401 Unauthorized by refreshing token
     */
    private static class TokenAuthenticator implements Authenticator {
        private final SessionManager sessionManager;

        public TokenAuthenticator(SessionManager sessionManager) {
            this.sessionManager = sessionManager;
        }

        @Override
        public Request authenticate(Route route, @NonNull Response response) throws IOException {
            // Synchronized block to prevent "Refresh Storms"
            synchronized (this) {
                String currentToken = sessionManager.getAccessToken();

                // If the token was already updated by another thread, retry with it
                String authHeader = response.request().header("Authorization");
                if (currentToken != null && authHeader != null && !authHeader.endsWith(currentToken)) {
                    return response.request().newBuilder()
                            .header("Authorization", "Bearer " + currentToken)
                            .build();
                }

                String refreshToken = sessionManager.getRefreshToken();
                if (refreshToken == null) {
                    return null; // Can't refresh
                }

                // Call refresh API synchronously
                AuthApi authApi = new Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                        .create(AuthApi.class);

                Call<AuthResponse<AuthData>> call = authApi.refresh(new RefreshRequest(refreshToken));
                retrofit2.Response<AuthResponse<AuthData>> refreshResponse = call.execute();

                if (refreshResponse.isSuccessful() && refreshResponse.body() != null && refreshResponse.body().isSuccess()) {
                    AuthData data = refreshResponse.body().getData();
                    sessionManager.updateTokens(data.getAccessToken(), data.getRefreshToken());

                    return response.request().newBuilder()
                            .header("Authorization", "Bearer " + data.getAccessToken())
                            .build();
                } else {
                    // Refresh failed, logout user
                    sessionManager.clearSession();
                    // In a real app, you might want to trigger a navigation to Login here
                    return null;
                }
            }
        }
    }
}
