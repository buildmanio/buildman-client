package io.buildman;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent;
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent;
import com.intellij.util.SmartList;
import io.buildman.common.models.*;
import io.buildman.common.utils.Utils;
import io.buildman.configuration.BuildmanAndroidRunConfigurationType;
import io.buildman.sync.FileAccess;
import io.buildman.common.utils.IdeUtils;
import io.buildman.common.utils.L;
import io.buildman.http.HttpServices;
import io.buildman.http.RetryableCallback;
import io.buildman.notification.Notifier;
import io.buildman.sync.FileRepository;
import io.buildman.sync.OnFilesSyncedListener;
import io.buildman.sync.SyncFilesHandler;
import io.buildman.task.BuildmanBuildTask;
import io.sentry.Sentry;
import org.apache.commons.lang3.SystemUtils;
import org.jetbrains.annotations.NotNull;
import retrofit2.Call;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class BuildmanService {

    private final String rootPath;
    private final Project project;

    public BuildmanService(Project project) {
        Sentry.init(options -> {
            options.setDsn(System.getProperty("SENTRY_DSN", ""));
            options.setEnableExternalConfiguration(true);
        });

        this.project = project;
        rootPath = Utils.getBasePath(project);
        String legacyUUID = Utils.getUUIDIfExist(rootPath + File.separator + ".idea" + File.separator + "buildman.id");
        if (legacyUUID != null) {
            BuildmanUser.getInstance().saveLicenseKey(legacyUUID);
        }
        BuildmanUser.getInstance().setProjectId(project.getName().replace(" ", "-") + "-" + BuildmanUser.getInstance().getLicenseKey());

        Sentry.setTag("ProjectName", BuildmanUser.getInstance().getProjectId());
        Sentry.setTag("LicenseKey", BuildmanUser.getInstance().getLicenseKey());
        Sentry.setTag("Version", IdeUtils.getPluginVersion());

        initRequests(true);
        registerFileWatcher();
        createConfiguration();

    }

    public void initRequests(boolean withSync) {
        if (BuildmanUser.getInstance().getLicenseKey() != null) {
            getServerChecksums(withSync);
            sendLocalChecksums();
        }
    }

    private void createConfiguration() {
        final List<BeforeRunTask<?>> makeTasks = new SmartList<>();
        makeTasks.add(new BuildmanBuildTask());
        BuildmanAndroidRunConfigurationType configType = BuildmanAndroidRunConfigurationType.getInstance();
        RunnerAndConfigurationSettings config = RunManager.getInstance(project).createConfiguration("Buildman", configType.getFactory());
        config.getConfiguration().setBeforeRunTasks(makeTasks);

        if (RunManager.getInstance(project).getConfigurationSettingsList(configType).size() == 0) {
            RunManager.getInstance(project).addConfiguration(config);
        }
    }

    private void registerFileWatcher() {
        project.getMessageBus().connect().subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
            @Override
            public void after(@NotNull List<? extends VFileEvent> events) {
                for (VFileEvent event : events) {
                    if (event.getPath().contains(".git/HEAD")) {
                        syncAll(null);
                        break;
                    }
                    Path path = Paths.get(event.getPath());
                    String directory = project.getBasePath();

                    if (FileAccess.isOutputFile(directory, path)) {
                        String checksum = FileAccess.fileChecksum(path);
                        String relativePath = FileAccess.toRelative(directory, path.toString());
                        if (event instanceof VFileDeleteEvent) {
                            FileRepository.getInstance()
                                    .getBuildDirectoryChecksums(rootPath)
                                    .remove(relativePath);
                            continue;
                        }
                        FileRepository.getInstance().getBuildDirectoryChecksums(rootPath).put(relativePath, checksum);
                    }

                    BuildCommand buildCommand = IdeUtils.getBuildCommand(project);
                    if (SyncFilesHandler.getInstance().isEligibleFile(directory, path) && buildCommand != null) {
                        HashMap<String, String> checksums = FileRepository.getInstance().getSourceCodeChecksumsInServer();
                        if (event instanceof VFilePropertyChangeEvent) {
                            String oldPath = FileAccess.toRelative(rootPath, ((VFilePropertyChangeEvent) event).getOldPath());
                            String newPath = FileAccess.toRelative(rootPath, ((VFilePropertyChangeEvent) event).getNewPath());
                            SyncFilesHandler
                                    .getInstance()
                                    .syncFile(new SyncableFile(ChangeType.MOVE, oldPath, newPath), newPath, buildCommand);
                            checksums.remove(oldPath);
                            checksums.put(newPath, FileAccess.fileChecksum(Paths.get(event.getPath())));
                            L.logger.info("Move : " + "Old path " + oldPath + " New path " + newPath);
                            continue;
                        }
                        if (event instanceof VFileMoveEvent) {
                            String oldPath = FileAccess.toRelative(rootPath, ((VFileMoveEvent) event).getOldPath());
                            String newPath = FileAccess.toRelative(rootPath, ((VFileMoveEvent) event).getNewPath());
                            SyncFilesHandler
                                    .getInstance()
                                    .syncFile(new SyncableFile(ChangeType.MOVE, oldPath, newPath), newPath, buildCommand);
                            checksums.remove(oldPath);
                            checksums.put(newPath, FileAccess.fileChecksum(Paths.get(event.getPath())));
                            L.logger.info("Move : " + "Old path " + oldPath + " New path " + newPath);
                            continue;
                        }
                        if (event instanceof VFileDeleteEvent) {
                            SyncFilesHandler
                                    .getInstance()
                                    .syncFile(new SyncableFile(ChangeType.DELETE, FileAccess.toRelative(rootPath, event.getPath()),
                                            FileAccess.toUnixPath(event.getPath())), null, buildCommand);
                            checksums.remove(FileAccess.toRelative(rootPath, event.getPath()));
                            L.logger.info("Delete " + FileAccess.toUnixPath(event.getPath()));
                            continue;
                        }
                        SyncFilesHandler
                                .getInstance()
                                .syncFile(new SyncableFile(ChangeType.MODIFY, FileAccess.toRelative(rootPath, event.getPath()),
                                        FileAccess.toUnixPath(event.getPath())), FileAccess.toUnixPath(event.getPath()), buildCommand);
                        checksums.put(FileAccess.toRelative(rootPath, event.getPath()), FileAccess.fileChecksum(Paths.get(event.getPath())));
                        L.logger.info("Modified " + FileAccess.toUnixPath(event.getPath()));
                    }
                }
            }
        });
    }

    private void sendLocalChecksums() {
        HashMap<String, String> checksums = FileAccess.calculateChecksum(rootPath);
        HttpServices.getInstance().services.putChecksum(checksums).enqueue(new RetryableCallback<String>() {
            @Override
            public void onSuccess(Call<BaseResponse<String>> call, BaseResponse<String> response) {

            }

            @Override
            public void onFail(Call<BaseResponse<String>> call, String message) {

            }
        });
    }

    private void getServerChecksums(boolean withSync) {
        HttpServices.getInstance().services.getChecksum().enqueue(new RetryableCallback<HashMap<String, String>>() {
            @Override
            public void onSuccess(Call<BaseResponse<HashMap<String, String>>> call, BaseResponse<HashMap<String, String>> response) {
                FileRepository.getInstance().setSourceCodeChecksumsInServer(response.data);
                L.logger.info("Checksum received with " + response.data.size() + " items");
                if (withSync) {
                    syncAll(null);
                }
            }

            @Override
            public void onFail(Call<BaseResponse<HashMap<String, String>>> call, String message) {
                Notifier.notifyToolWindow(project, message);
            }
        });
    }

    public void syncAll(OnFilesSyncedListener listener) {
        String basePath = SystemUtils.IS_OS_WINDOWS ? project.getBasePath().replace("/", "\\") : project.getBasePath();

        ApplicationManager.getApplication().invokeLater(() -> {
            if (FileDocumentManager.getInstance().getUnsavedDocuments().length > 0) {
                FileDocumentManager.getInstance().saveAllDocuments();
            }
        });

        L.logger.info("Start Syncing");

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Syncing files ...") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText("Buildman: Syncing files...");
                SyncFilesHandler.getInstance().syncAll(basePath);

                while (SyncFilesHandler.getInstance().isSyncHappening()) {
                    try {
                        Thread.sleep(100);
                        indicator.checkCanceled();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Sentry.captureException(e);
                    }
                }
                if (listener != null) {
                    listener.onFinished();
                }
            }
        });
    }
}
