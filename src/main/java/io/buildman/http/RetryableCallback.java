package io.buildman.http;

import io.buildman.common.models.BaseResponse;
import org.jetbrains.annotations.NotNull;
import retrofit2.Call;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public abstract class RetryableCallback<T> extends BuildmanCallback<T> {

    private static final int RETRY_COUNT = 3;
    /**
     * Base retry delay for exponential backoff, in Milliseconds
     */
    private static final double RETRY_DELAY = 300;
    private int retryCount = 0;
    private final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);

    @Override
    public void onFailure(@NotNull final Call<BaseResponse<T>> call, Throwable t) {
        retryCount++;
        if (retryCount <= RETRY_COUNT) {
            int expDelay = (int) (RETRY_DELAY * Math.pow(2, Math.max(0, retryCount - 1)));

            scheduledThreadPoolExecutor.schedule(() -> retry(call), expDelay, TimeUnit.MILLISECONDS);
        } else {
            onFail(call, "Request failed!");
        }
    }

    private void retry(@NotNull final Call<BaseResponse<T>> call) {
        call.clone().enqueue(this);
    }

}
