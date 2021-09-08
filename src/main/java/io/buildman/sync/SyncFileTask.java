package io.buildman.sync;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.buildman.common.models.BaseResponse;
import io.buildman.common.models.BuildCommand;
import io.buildman.common.models.ChangeType;
import io.buildman.common.models.SyncableFile;
import io.buildman.common.utils.PayloadSerializer;
import io.buildman.http.HttpServices;
import io.sentry.Sentry;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Response;

import java.io.File;
import java.io.IOException;

public class SyncFileTask implements Runnable {
    private final SyncableFile syncableFile;
    private final File file;
    private BuildCommand buildCommand;
    private boolean retried = false;

    public SyncFileTask(SyncableFile syncableFile, File file) {
        this.syncableFile = syncableFile;
        this.file = file;
    }

    public SyncFileTask(SyncableFile syncableFile, File file, BuildCommand buildCommand) {
        this.syncableFile = syncableFile;
        this.file = file;
        this.buildCommand = buildCommand;
    }

    @Override
    public void run() {
        MultipartBody.Part filePart = null;
        if (file != null && syncableFile.changeType != ChangeType.DELETE) {
            filePart = MultipartBody.Part.createFormData("file", file.getName(), RequestBody.create(MediaType.parse("text/*"), file));
        }
        MultipartBody.Part metaPart = null;
        MultipartBody.Part commandPart = null;
        try {
            metaPart = MultipartBody.Part.createFormData("meta", "meta", RequestBody.create(MediaType.parse("application/json"), PayloadSerializer.mapper.writeValueAsString(syncableFile).getBytes()));
            commandPart = MultipartBody.Part.createFormData("command", "command", RequestBody.create(MediaType.parse("application/json"), PayloadSerializer.mapper.writeValueAsString(buildCommand).getBytes()));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            Sentry.captureException(e);
        }

        try {
            Response<BaseResponse<String>> response = HttpServices.getInstance().services.sync(filePart, metaPart,commandPart).execute();
            boolean hasProblem = response.body() == null || !response.isSuccessful() || !response.body().success;
            if (hasProblem && !retried) {
                retried = true;
                run();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Sentry.captureException(e);
            if (!retried) {
                retried = true;
                run();
            }
        }
    }
}
