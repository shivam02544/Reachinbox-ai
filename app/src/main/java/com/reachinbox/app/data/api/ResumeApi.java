package com.reachinbox.app.data.api;

import com.reachinbox.app.data.model.ResumeResponse;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ResumeApi {
    @GET("ai/resume")
    Call<ResumeResponse> getResume();

    @Multipart
    @POST("ai/resume")
    Call<ResumeResponse> uploadResume(@Part MultipartBody.Part file);

    @DELETE("ai/resume")
    Call<ResumeResponse> deleteResume();
}
