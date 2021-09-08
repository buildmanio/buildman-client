package io.buildman.sync;

import io.buildman.common.models.*;
import io.buildman.common.utils.L;
import io.sentry.Sentry;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

public class SyncFilesHandler {
    private final HashSet<String> sendingFilesChecksums = new HashSet<>();
    public final ThreadPoolExecutor threadPool;

    private SyncFilesHandler() {
        threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
    }

    private static class Holder {
        private static final SyncFilesHandler INSTANCE = new SyncFilesHandler();
    }

    public static SyncFilesHandler getInstance() {
        return SyncFilesHandler.Holder.INSTANCE;
    }

    public boolean syncAll(String directory) {
        if (BuildmanUser.getInstance().getLicenseKey() == null) {
            return false;
        }
        long startTime = System.currentTimeMillis();
        AtomicBoolean hasSyncingFiles = new AtomicBoolean(false);
        HashMap<String, String> tempChecksums = new HashMap<>(FileRepository.getInstance().getSourceCodeChecksumsInServer());
        try {
            Files.walk(Paths.get(directory))
                    .filter(path -> isEligibleFile(directory, path))
                    .parallel()
                    .forEach(path -> {
                        String relativePath = FileAccess.toRelative(directory, path.toString());
                        HashMap<String, String> checksums = FileRepository.getInstance().getSourceCodeChecksumsInServer();
                        String checksum = checksums.get(relativePath);
                        String currentChecksum = FileAccess.fileChecksum(path);
                        tempChecksums.remove(relativePath);
                        if (checksum != null && checksum.equals(currentChecksum)) {
                            return;
                        }
                        FileRepository.getInstance().addSendingFilesChecksum(currentChecksum);
                        SyncableFile syncableFile = new SyncableFile(ChangeType.MODIFY, relativePath, relativePath);
                        syncFile(syncableFile, path.toFile());
                        checksums.put(relativePath, currentChecksum);
                        hasSyncingFiles.set(true);
                        L.logger.info("Sent file ----- " + relativePath + " check: " + currentChecksum);
                    });
        } catch (IOException e) {
            e.printStackTrace();
            Sentry.captureException(e);
        }
        removeFiles(tempChecksums);
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        L.logger.info("Initial sync time  " + elapsedTime);
        return hasSyncingFiles.get();
    }

    public boolean isSyncHappening() {
        return threadPool.getActiveCount() > 0;
    }

    public void syncFile(SyncableFile meta, File file) {
        threadPool.submit(new SyncFileTask(meta, file));
    }

    public void syncFile(SyncableFile meta, String filePath, BuildCommand buildCommand) {
        File file = null;
        if (filePath != null) {
            file = new File(filePath);
        }
        threadPool.submit(new SyncFileTask(meta, file, buildCommand));
    }

    private void removeFiles(HashMap<String, String> tempChecksums) {
        for (String key : tempChecksums.keySet()) {
            syncFile(new SyncableFile(ChangeType.DELETE, key, key), null);
            FileRepository.getInstance().getSourceCodeChecksumsInServer().remove(key);
        }
    }

    public boolean isEligibleFile(String baseDirectory, Path path) {
        PathRules pathRules = new PathRules();
        pathRules.addRule(".git/");
        pathRules.addRule(".idea/");
        pathRules.addRule(".gradle/");
        pathRules.addRule("build/");
        pathRules.addRule("local.properties");
        pathRules.addRule(".apk");
        pathRules.addRule(".hprof");
        File file = path.toFile();
        boolean ignored = pathRules.matches(FileAccess.toRelative(baseDirectory, path.toString()), file.isDirectory());
        return !ignored && !Files.isDirectory(path);
    }
}
