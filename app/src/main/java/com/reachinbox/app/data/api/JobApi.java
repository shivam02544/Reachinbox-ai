package com.reachinbox.app.data.api;

import com.reachinbox.app.data.model.CreateJobRequest;
import com.reachinbox.app.data.model.JobFromUrlRequest;
import com.reachinbox.app.data.model.JobFromUrlResponse;
import com.reachinbox.app.data.model.JobRequirementResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface JobApi {
    @POST("ai/job")
    Call<JobRequirementResponse> createJobRequirement(@Body CreateJobRequest request);

    @POST("ai/job/from-url")
    Call<JobFromUrlResponse> createFromUrl(@Body JobFromUrlRequest request);
}
