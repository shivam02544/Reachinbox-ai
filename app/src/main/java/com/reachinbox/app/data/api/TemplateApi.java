package com.reachinbox.app.data.api;

import com.reachinbox.app.data.model.AuthResponse;
import com.reachinbox.app.data.model.CreateTemplateRequest;
import com.reachinbox.app.data.model.Template;
import com.reachinbox.app.data.model.TemplateListResponse;
import com.reachinbox.app.data.model.UpdateTemplateRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TemplateApi {
    @POST("templates")
    Call<AuthResponse<Template>> createTemplate(@Body CreateTemplateRequest request);

    @GET("templates")
    Call<AuthResponse<TemplateListResponse>> getTemplates(
            @Query("page") int page,
            @Query("limit") int limit,
            @Query("search") String search,
            @Query("sortBy") String sortBy,
            @Query("sortOrder") String sortOrder
    );

    @GET("templates/{id}")
    Call<AuthResponse<Template>> getTemplate(@Path("id") String id);

    @PATCH("templates/{id}")
    Call<AuthResponse<Template>> updateTemplate(@Path("id") String id, @Body UpdateTemplateRequest request);

    @DELETE("templates/{id}")
    Call<AuthResponse<Void>> deleteTemplate(@Path("id") String id);
}
