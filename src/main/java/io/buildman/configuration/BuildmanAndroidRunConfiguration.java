package io.buildman.configuration;


import com.android.tools.idea.run.AndroidRunConfiguration;
import com.android.tools.idea.run.AndroidRunConfigurationBase;
import com.android.tools.idea.run.ApkProvider;
import com.android.tools.idea.run.editor.DeployTargetContext;
import com.android.tools.idea.run.ui.BaseAction;
import com.android.tools.idea.stats.RunStats;
import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.JavaRunConfigurationModule;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import io.buildman.common.utils.IdeUtils;
import io.buildman.common.utils.L;
import io.buildman.common.utils.Utils;
import io.buildman.notification.Notifier;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.util.AndroidUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

public class BuildmanAndroidRunConfiguration extends AndroidRunConfiguration {

    private final Project project;

    public BuildmanAndroidRunConfiguration(Project project, ConfigurationFactory factory) {
        super(project, factory);
        this.project = project;
        putUserData(BaseAction.SHOW_APPLY_CHANGES_UI, false);
    }


    @Override
    protected @Nullable
    ApkProvider getApkProvider() {
        List<AndroidFacet> facets = AndroidUtils.getApplicationFacets(project);
        if (facets.isEmpty()) {
            L.logger.error("Facet is not set!");
            return null;
        }


        return new BuildmanApkProvider(facets.get(0), IdeUtils.getBuildCommand(project), Utils.getBasePath(project));
    }

    @Override
    public @Nullable
    Icon getExecutorIcon(@NotNull RunConfiguration configuration, @NotNull Executor executor) {
        putUserData(BaseAction.SHOW_APPLY_CHANGES_UI, false);
        return super.getExecutorIcon(configuration, executor);
    }
}
