package io.buildman.http;

import io.buildman.common.models.BaseResponse;
import org.jetbrains.annotations.NotNull;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public abstract class BuildmanCallback<T> implements Callback<BaseResponse<T>> {
    @Override
    public void onResponse(@NotNull Call<BaseResponse<T>> call, @NotNull Response<BaseResponse<T>> response) {
        if (response.body() != null && response.isSuccessful() && response.body().success) {
            onSuccess(call, response.body());
            return;
        }
        if (response.body() != null) {
            onFail(call, (String) response.body().data);
            return;
        }
        onFail(call, "No response");
    }

    @Override
    public void onFailure(@NotNull Call<BaseResponse<T>> call, Throwable t) {
        t.printStackTrace();
        if (!call.isCanceled())
            onFail(call, "Request failed");
    }

    public abstract void onSuccess(Call<BaseResponse<T>> call, BaseResponse<T> response);

    public abstract void onFail(Call<BaseResponse<T>> call, String message);
}
