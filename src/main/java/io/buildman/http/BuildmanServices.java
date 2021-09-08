package io.buildman.http;

import io.buildman.common.models.BuildCommand;
import io.buildman.common.models.BaseResponse;
import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.HashMap;

public interface BuildmanServices {

    @POST("/build")
    Call<BaseResponse<String>> build(@Body BuildCommand buildCommand);

    @POST("/checksum")
    Call<BaseResponse<String>> putChecksum(@Body HashMap<String, String> map);

    @GET("/checksum")
    Call<BaseResponse<HashMap<String, String>>> getChecksum();

    @Multipart
    @POST("/sync")
    Call<BaseResponse<String>> sync(@Part MultipartBody.Part file, @Part MultipartBody.Part meta,@Part MultipartBody.Part command);

    @Streaming
    @GET("/download")
    Call<ResponseBody> download(@Query("path") String path);


}
