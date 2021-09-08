package io.buildman.common.utils;

import io.buildman.common.models.BuildCommand;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.List;

public class GradleUtils {

    private static String apkPathFromGradleTask(String rootPath, BuildCommand buildCommand) {
        String path = rootPath + File.separator + buildCommand.getGroupId() + File.separator + buildCommand.getModuleName() + "/build/outputs/apk/";
        StringBuilder folderName = new StringBuilder();
        List<String> flavors = buildCommand.getProductFlavor();
        for (int i = 0; i < flavors.size(); i++) {
            if (i == 0) {
                folderName.append(flavors.get(i));
            } else {
                folderName.append(Utils.capitalize(flavors.get(i)));
            }
        }
        String flavorsName = String.join("-", flavors);
        StringBuilder apkName = new StringBuilder();
        apkName.append(buildCommand.getModuleName());
        if (flavors.size() > 0 && !flavorsName.isEmpty()) {
            apkName.append("-");
            apkName.append(flavorsName);
        }
        apkName.append("-");
        apkName.append(buildCommand.getBuildType());
        apkName.append(".apk");

        return path + folderName + File.separator + buildCommand.getBuildType() + File.separator + apkName;
    }


    public static String apkDirectoryPath(String rootPath, BuildCommand buildCommand) {
        String apkPath = apkPathFromGradleTask(rootPath, buildCommand);

        return FilenameUtils.getFullPath(apkPath);
    }
}
