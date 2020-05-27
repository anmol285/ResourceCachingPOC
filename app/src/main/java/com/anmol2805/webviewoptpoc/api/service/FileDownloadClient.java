package com.anmol2805.webviewoptpoc.api.service;

import java.util.List;
import java.util.Set;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

public interface FileDownloadClient {

    @GET()
    Call<ResponseBody> downloadFile(@Url String url);

    @GET("resurls")
    Call<Set<String>> getResUrls();
}
