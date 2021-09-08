package io.buildman.task;


import com.android.tools.idea.stats.RunStats;
import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.util.Key;
import icons.Icons;
import io.buildman.common.utils.L;
import io.buildman.configuration.BuildmanAndroidRunConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class BuildmanTaskProvider extends BeforeRunTaskProvider<BuildmanBuildTask> {
    @NotNull
    public static final Key<BuildmanBuildTask> ID = Key.create("Android.Gradle.BuildmanTask");
    public static final String TASK_NAME = "Buildman Make";

    @Override
    public Key<BuildmanBuildTask> getId() {
        return ID;
    }

    @Override
    public String getName() {
        return TASK_NAME;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public @Nullable
    Icon getIcon() {
        return Icons.run_icon;
    }

    @Override
    public @Nullable
    BuildmanBuildTask createTask(@NotNull RunConfiguration runConfiguration) {
        if (runConfiguration instanceof BuildmanAndroidRunConfiguration) {
            BuildmanBuildTask task = new BuildmanBuildTask();
            task.setEnabled(true);

            return task;
        }

        return null;
    }

    @Override
    public boolean executeTask(@NotNull DataContext dataContext, @NotNull RunConfiguration runConfiguration, @NotNull ExecutionEnvironment executionEnvironment, @NotNull BuildmanBuildTask buildmanBuildTask) {
        RunStats stats = RunStats.from(executionEnvironment);
        try {
            stats.beginBeforeRunTasks();
            return doExecuteTask(dataContext, runConfiguration, executionEnvironment, buildmanBuildTask);
        } finally {
            stats.endBeforeRunTasks();
        }
    }

    private boolean doExecuteTask(DataContext context, RunConfiguration configuration, ExecutionEnvironment env, BuildmanBuildTask buildmanBuildTask) {
        L.logger.info("Start Doing the task");
        BuildmanTaskExecutor buildmanTaskExecutor = new BuildmanTaskExecutor(configuration.getProject());

        return buildmanTaskExecutor.executeSync();
    }
}
