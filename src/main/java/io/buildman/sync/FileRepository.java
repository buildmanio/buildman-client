package io.buildman.sync;

import java.util.HashMap;
import java.util.HashSet;

public class FileRepository {

    private FileRepository() {
    }

    private static class Holder {
        private static final FileRepository INSTANCE = new FileRepository();
    }

    public static FileRepository getInstance() {
        return Holder.INSTANCE;
    }

    public HashMap<String, String> getSourceCodeChecksumsInServer() {
        return sourceCodeChecksumsInServer;
    }

    private HashMap<String, String> sourceCodeChecksumsInServer = new HashMap<>();

    public HashMap<String, String> getBuildDirectoryChecksums(String path) {
        if (!buildDirectoryChecksums.isEmpty()) {
            return buildDirectoryChecksums;
        }

        return FileAccess.calculateChecksum(path);
    }

    private final HashMap<String, String> buildDirectoryChecksums = new HashMap<>();
    private final HashSet<String> sendingFilesChecksums = new HashSet<>();


    public void putSourceCodeChecksumsInServer(HashMap<String, String> sourceCodeChecksumsInServer) {
        this.sourceCodeChecksumsInServer.putAll(sourceCodeChecksumsInServer);
    }
    public void setSourceCodeChecksumsInServer(HashMap<String, String> sourceCodeChecksumsInServer) {
        this.sourceCodeChecksumsInServer = sourceCodeChecksumsInServer;
    }

    public HashSet<String> getSendingFilesChecksums() {
        return sendingFilesChecksums;
    }

    public void addSendingFilesChecksum(String checksum) {
        this.sendingFilesChecksums.add(checksum);
    }

}
