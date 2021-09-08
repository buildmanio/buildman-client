package io.buildman.task;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import io.buildman.BuildmanService;
import io.buildman.common.models.BaseResponse;
import io.buildman.common.models.BuildCommand;
import io.buildman.common.models.BuildStep;
import io.buildman.common.models.BuildmanUser;
import io.buildman.common.utils.*;
import io.buildman.http.BuildmanCallback;
import io.buildman.http.HttpServices;
import io.buildman.notification.Notifier;
import io.buildman.sync.FileAccess;
import io.sentry.Sentry;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import retrofit2.Call;
import retrofit2.Callback;

import java.awt.*;
import java.io.File;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class BuildmanTaskExecutor {
    private final Project project;

    private long buildStartTime;
    private long serverBuildDuration;
    private long downloadDuration;
    private BuildCommand currentBuildCommand;
    private String basePath;
    private Call<BaseResponse<String>> buildCommandCall;
    private Call<ResponseBody> downloadCall;
    private BuildStep currentBuildStep;
    private final CountDownLatch doneSignal = new CountDownLatch(1);

    public BuildmanTaskExecutor(Project project) {
        this.project = project;

    }

    public boolean executeSync() {
        if (!check()) {
            return false;
        }
        startBuildSteps();
        try {
            doneSignal.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return currentBuildStep != BuildStep.DONE_ERROR;
    }

    private boolean check() {
        basePath = Utils.getBasePath(project);
        if (currentBuildStep != null && currentBuildStep != BuildStep.DONE) {
            Notifier.notifyToolWindow(project, "You can't run multiple build, please cancel build first");
            return false;
        }

        if (BuildmanUser.getInstance().getLicenseKey() == null) {
            AtomicBoolean saved = new AtomicBoolean(false);
            ApplicationManager.getApplication().invokeAndWait(() -> {
                AskLicenseKeyDialog dialog = new AskLicenseKeyDialog();
                if (dialog.showAndGet()) {
                    String licenceKey = dialog.trialClicked ? Utils.generateUDID(basePath + File.separator + ".idea" + File.separator + "buildman.id")
                            : dialog.askLicenseKey.licenseKey.getText();
                    BuildmanUser.getInstance().saveLicenseKey(licenceKey);
                    BuildmanUser.getInstance().setProjectId(project.getName().replace(" ", "-") + "-" + BuildmanUser.getInstance().getLicenseKey());
                    saved.set(true);
                    HttpServices.getInstance().reCreateClient();
                    Sentry.setTag("ProjectName", BuildmanUser.getInstance().getProjectId());
                    Sentry.setTag("LicenseKey", BuildmanUser.getInstance().getLicenseKey());

                    BuildmanService buildmanService = project.getService(BuildmanService.class);
                    buildmanService.initRequests(false);
                }
            });
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return saved.get();
        }

        return true;
    }

    private void startBuildSteps() {
        buildStartTime = System.currentTimeMillis();
        BuildmanService buildmanService = project.getService(BuildmanService.class);
        buildmanService.syncAll(() -> {
            sendBuildCommand();
            startProgressBar();
        });
    }

    private void sendBuildCommand() {
        BuildCommand buildCommand = IdeUtils.getBuildCommand(project);
        currentBuildCommand = buildCommand;
        L.logger.info("Sending build payload command");
        changeState(BuildStep.BUILDING);
        buildCommandCall = HttpServices.getInstance().services.build(buildCommand);

        buildCommandCall.enqueue(new BuildmanCallback<String>() {
            @Override
            public void onSuccess(Call<BaseResponse<String>> call, BaseResponse<String> response) {
                download(response.data);
                serverBuildDuration = System.currentTimeMillis() - buildStartTime;
                L.logger.info("Client build time " + serverBuildDuration);
            }

            @Override
            public void onFail(Call<BaseResponse<String>> call, String message) {
                Notifier.notifyToolWindow(project, message);
                changeState(BuildStep.DONE_ERROR);
            }
        });
    }

    private void download(String data) {
        L.logger.info("Start downloading ...");
        String filePath = new String(Base64.getDecoder().decode(data.getBytes()));
        if (filePath.contains("diff")) {
            changeState(BuildStep.DOWNLOADING_DIFF);
        } else {
            changeState(BuildStep.DOWNLOADING_APK);
        }

        downloadCall = HttpServices.getInstance().services.download(data);
        downloadCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NotNull Call<ResponseBody> call, @NotNull retrofit2.Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    FileAccess.writeResponseBodyToDisk(response.body(), Paths.get(basePath + File.separator + filePath));
                    String apkDirectory = GradleUtils.apkDirectoryPath(basePath, currentBuildCommand);
                    String apkPath = apkDirectory + "new/app.apk";
                    if (filePath.contains("diff")) {
                        if (!ApkUtils.patchApk(apkPath, apkDirectory + File.separator + "diff")) {
                            changeState(BuildStep.DONE_ERROR);
                            Notifier.notifyToolWindow(project, "Failed to patch APK!");
                            return;
                        }
                    }
                    downloadDuration = (System.currentTimeMillis() - buildStartTime) - serverBuildDuration;
                    endBuildNotification();
                    changeState(BuildStep.DONE);
                    sendLocalChecksum();
                } else {
                    Notifier.notifyToolWindow(project, "Failed to download APK!");
                    changeState(BuildStep.DONE_ERROR);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Notifier.notifyToolWindow(project, "Failed to download APK!");
                currentBuildStep = BuildStep.DONE_ERROR;
            }
        });
    }

    private void endBuildNotification() {
        String completeText = "Build complete in " + Utils.humanReadableFormatTime(Duration.ofMillis(System.currentTimeMillis() - buildStartTime));
        completeText += " | Server: " + Utils.humanReadableFormatTime(Duration.ofMillis(serverBuildDuration));
        completeText += " | Download: " + Utils.humanReadableFormatTime(Duration.ofMillis(downloadDuration));
        Notifier.notifyToolWindow(project, completeText);
    }

    private void sendLocalChecksum() {
        HashMap<String, String> checksums = FileAccess.calculateChecksum(basePath);
        HttpServices.getInstance().services.putChecksum(checksums).enqueue(new BuildmanCallback<String>() {
            @Override
            public void onSuccess(Call<BaseResponse<String>> call, BaseResponse<String> response) {
                L.logger.info("Local checksum sent.");
            }

            @Override
            public void onFail(Call<BaseResponse<String>> call, String message) {
                L.logger.info("Local checksum  failed.");
            }
        });
    }


    private void cancelCalls() {
        if (downloadCall != null) {
            downloadCall.cancel();
        }

        if (buildCommandCall != null) {
            buildCommandCall.cancel();
        }
        changeState(BuildStep.DONE_ERROR);
    }

    private void changeState(BuildStep buildStep) {
        currentBuildStep = buildStep;
        if (currentBuildStep == BuildStep.DONE || currentBuildStep == BuildStep.DONE_ERROR) {
            doneSignal.countDown();
        }
    }

    private void startProgressBar() {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Building with buildman ... ") {
            public void run(@NotNull ProgressIndicator progressIndicator) {
                progressIndicator.setIndeterminate(true);
                while (currentBuildStep == BuildStep.BUILDING || currentBuildStep == BuildStep.DOWNLOADING_APK || currentBuildStep == BuildStep.DOWNLOADING_DIFF) {
                    progressIndicator.setText("Buildman: " + currentBuildStep.toString().replace("_", " ").toLowerCase() + "...");
                    try {
                        Thread.sleep(100);
                        try {
                            progressIndicator.checkCanceled();
                        } catch (ProcessCanceledException exception) {
                            cancelCalls();
                            progressIndicator.cancel();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Sentry.captureException(e);
                    }
                }

            }
        });
    }
}
