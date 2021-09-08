package io.buildman.task;

import com.android.tools.idea.gradle.run.MakeBeforeRunTask;
import com.intellij.execution.BeforeRunTask;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;

public class BuildmanBuildTask extends BeforeRunTask<BuildmanBuildTask> {
    public BuildmanBuildTask() {
        super(BuildmanTaskProvider.ID);
    }
}
