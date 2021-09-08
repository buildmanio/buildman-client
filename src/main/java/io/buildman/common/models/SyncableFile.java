package io.buildman.common.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;

public class SyncableFile implements Serializable {

    public ChangeType changeType;
    public String path;
    public String newPath;

    public SyncableFile() {
    }

    public SyncableFile(ChangeType changeType, String path, String newPath) {
        this.changeType = changeType;
        this.path = path;
        this.newPath = newPath;
    }
}
