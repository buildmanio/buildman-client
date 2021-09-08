package io.buildman.configuration;

import com.android.ddmlib.IDevice;
import com.android.tools.idea.gradle.project.model.AndroidModuleModel;
import com.android.tools.idea.run.ApkInfo;
import com.android.tools.idea.run.ApkProvider;
import com.android.tools.idea.run.ApkProvisionException;
import com.android.tools.idea.run.ValidationError;
import com.google.common.collect.ImmutableList;
import io.buildman.common.models.BuildCommand;
import io.buildman.common.utils.GradleUtils;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BuildmanApkProvider implements ApkProvider {
    private final AndroidFacet facet;
    private final BuildCommand buildCommand;
    private final String basePath;

    public BuildmanApkProvider(@NotNull AndroidFacet facet, BuildCommand buildCommand, String basePath) {
        this.facet = facet;
        this.buildCommand = buildCommand;
        this.basePath = basePath;
    }

    @Override
    public @NotNull
    Collection<ApkInfo> getApks(@NotNull IDevice iDevice) throws ApkProvisionException {
        List<ApkInfo> apkList = new ArrayList<>();
        String packageName = AndroidModuleModel.get(facet).getApplicationId();

        String apkDirectory = GradleUtils.apkDirectoryPath(basePath, buildCommand );
        String apkPath = apkDirectory + "new/app.apk";
        File apkFile = new File(apkPath);
        apkList.add(new ApkInfo(apkFile, packageName));

        return apkList;
    }

    @Override
    public @NotNull
    List<ValidationError> validate() {
        return ImmutableList.of();
    }
}
