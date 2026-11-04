package com.reachinbox.app.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;

import com.reachinbox.app.data.api.AiComposeApi;
import com.reachinbox.app.data.api.ApiClient;
import com.reachinbox.app.data.model.ImproveEmailRequest;
import com.reachinbox.app.data.model.ImproveEmailResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AiComposeRepository {
    private final AiComposeApi api;

    public AiComposeRepository(Context context) {
        api = ApiClient.getClient(context).create(AiComposeApi.class);
    }

    public interface CallbackListener {
        void onSuccess(ImproveEmailResponse.Suggestion suggestion);
        void onError(String message);
    }

    public void improve(String subject, String body, String tone, CallbackListener listener) {
        api.improveEmail(new ImproveEmailRequest(subject, body, tone)).enqueue(new Callback<ImproveEmailResponse>() {
            @Override
            public void onResponse(@NonNull Call<ImproveEmailResponse> call,
                                   @NonNull Response<ImproveEmailResponse> response) {
                ImproveEmailResponse result = response.body();
                if (response.isSuccessful() && result != null && result.isSuccess() && result.getData() != null) {
                    listener.onSuccess(result.getData());
                } else {
                    listener.onError(result != null && result.getMessage() != null
                            ? result.getMessage() : "Could not improve this email.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ImproveEmailResponse> call, @NonNull Throwable error) {
                listener.onError(error instanceof java.net.SocketTimeoutException
                        ? "AI took too long to respond. Try again."
                        : "Unable to reach the AI service. Check your connection and try again.");
            }
        });
    }
}
