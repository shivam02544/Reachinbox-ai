package com.reachinbox.app.data.repository;

import android.content.Context;
import androidx.annotation.NonNull;

import com.reachinbox.app.data.api.ApiClient;
import com.reachinbox.app.data.api.ResumeApi;
import com.reachinbox.app.data.model.Resume;
import com.reachinbox.app.data.model.ResumeResponse;

import java.io.File;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResumeRepository {
    private final ResumeApi resumeApi;

    public ResumeRepository(Context context) {
        this.resumeApi = ApiClient.getClient(context).create(ResumeApi.class);
    }

    public interface ResumeCallback {
        void onSuccess(Resume resume);
        void onError(String message);
    }

    public interface DeleteCallback {
        void onSuccess();
        void onError(String message);
    }

    public void getResume(ResumeCallback callback) {
        resumeApi.getResume().enqueue(new Callback<ResumeResponse>() {
            @Override
            public void onResponse(@NonNull Call<ResumeResponse> call, @NonNull Response<ResumeResponse> response) {
                handleResponse(response, callback);
            }

            @Override
            public void onFailure(@NonNull Call<ResumeResponse> call, @NonNull Throwable t) {
                callback.onError(getFailureMessage(t));
            }
        });
    }

    public void uploadResume(File pdfFile, ResumeCallback callback) {
        RequestBody requestFile = RequestBody.create(pdfFile, MediaType.parse("application/pdf"));
        MultipartBody.Part body = MultipartBody.Part.createFormData("resume", pdfFile.getName(), requestFile);

        resumeApi.uploadResume(body).enqueue(new Callback<ResumeResponse>() {
            @Override
            public void onResponse(@NonNull Call<ResumeResponse> call, @NonNull Response<ResumeResponse> response) {
                handleResponse(response, callback);
            }

            @Override
            public void onFailure(@NonNull Call<ResumeResponse> call, @NonNull Throwable t) {
                callback.onError(getFailureMessage(t));
            }
        });
    }

    public void deleteResume(DeleteCallback callback) {
        resumeApi.deleteResume().enqueue(new Callback<ResumeResponse>() {
            @Override
            public void onResponse(@NonNull Call<ResumeResponse> call, @NonNull Response<ResumeResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    callback.onSuccess();
                } else {
                    callback.onError(getErrorMessage(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResumeResponse> call, @NonNull Throwable t) {
                callback.onError(getFailureMessage(t));
            }
        });
    }

    private void handleResponse(Response<ResumeResponse> response, ResumeCallback callback) {
        if (response.isSuccessful() && response.body() != null) {
            if (response.body().isSuccess()) {
                callback.onSuccess(response.body().getData());
            } else {
                callback.onError(response.body().getMessage() != null ? response.body().getMessage() : "Unknown error");
            }
        } else {
            callback.onError(getErrorMessage(response));
        }
    }

    private String getErrorMessage(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String errorStr = response.errorBody().string();
                com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseString(errorStr).getAsJsonObject();
                if (jsonObject.has("message") && !jsonObject.get("message").isJsonNull()) {
                    return jsonObject.get("message").getAsString();
                }
            }
        } catch (Exception ignored) { }
        return "Server error: " + response.code();
    }

    private String getFailureMessage(Throwable t) {
        if (t instanceof java.net.SocketTimeoutException) {
            return "Server connection timed out.";
        } else if (t instanceof java.io.IOException) {
            return "Network error. Please check your connection.";
        }
        return "Unknown error: " + t.getMessage();
    }
}
