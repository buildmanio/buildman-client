package io.buildman.http;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import io.buildman.BuildmanService;
import io.buildman.common.models.BuildmanUser;
import io.buildman.common.utils.IdeUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class HttpServices {

    public BuildmanServices services;

    private HttpServices() {
        reCreateClient();
    }

    private static class Holder {
        private static final HttpServices INSTANCE = new HttpServices();
    }

    public static HttpServices getInstance() {
        return HttpServices.Holder.INSTANCE;
    }

    public void reCreateClient() {
        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        httpClientBuilder.addInterceptor(chain -> {
            Request original = chain.request();

            Request request = original.newBuilder()
                    .header("X-Project-Id", BuildmanUser.getInstance().getProjectId())
                    .header("X-License-Key", BuildmanUser.getInstance().getLicenseKey())
                    .header("X-Client-Version", IdeUtils.getPluginVersion())
                    .method(original.method(), original.body())
                    .build();

            return chain.proceed(request);
        });
        httpClientBuilder
                .connectTimeout(2, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.MINUTES);
        OkHttpClient httpClient = httpClientBuilder.build();


        Retrofit retrofit = new Retrofit.Builder()
                .client(httpClient)
//                .baseUrl("http://localhost:8090")
                .baseUrl("https://api.buildman.cloud")
                .addConverterFactory(JacksonConverterFactory.create())
                .build();
        services = retrofit.create(BuildmanServices.class);
    }
}
