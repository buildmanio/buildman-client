package io.buildman.common.models;

import java.io.Serializable;
import java.util.List;

public class BuildCommand implements Serializable, Cloneable {
    private List<String> productFlavor;
    private String buildType;
    private String task;
    private String groupId;
    private String moduleName;

    public BuildCommand() {

    }

    public BuildCommand(List<String> productFlavor, String buildType, String task, String groupId, String moduleName) {
        this.productFlavor = productFlavor;
        this.buildType = buildType;
        this.task = task;
        this.groupId = groupId;
        this.moduleName = moduleName;
    }

    public List<String> getProductFlavor() {
        return productFlavor;
    }

    public String getBuildType() {
        return buildType;
    }

    public String getTask() {
        return task;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getModuleName() {
        return moduleName;
    }
}
