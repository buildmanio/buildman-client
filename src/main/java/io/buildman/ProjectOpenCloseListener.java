package io.buildman;


import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import io.buildman.BuildmanService;
import org.jetbrains.annotations.NotNull;

public class ProjectOpenCloseListener  implements ProjectManagerListener {
    @Override
    public void projectOpened(@NotNull Project project) {
        BuildmanService buildmanService = project.getService(BuildmanService.class);
    }
    @Override
    public void projectClosing(@NotNull Project project) {

    }
}

