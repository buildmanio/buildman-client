package io.buildman.common.utils;


import com.android.tools.idea.gradle.model.IdeVariant;
import com.android.tools.idea.gradle.project.model.AndroidModuleModel;
import com.intellij.execution.RunManager;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import io.buildman.common.models.BuildCommand;
import io.buildman.configuration.BuildmanAndroidRunConfiguration;
import io.buildman.configuration.BuildmanAndroidRunConfigurationType;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class IdeUtils {

    public static BuildCommand getBuildCommand(Project project) {
        if (!RunManager.getInstance(project).getSelectedConfiguration().getType().getId().equals(BuildmanAndroidRunConfigurationType.ID)) {
            return null;
        }
        @NotNull BuildmanAndroidRunConfiguration config = (BuildmanAndroidRunConfiguration) RunManager.getInstance(project).getSelectedConfiguration().getConfiguration();

        AndroidFacet facet = AndroidFacet.getInstance(config.getConfigurationModule().getModule());
        AndroidModuleModel androidModule = AndroidModuleModel.get(facet);
        if (androidModule == null) {
            return null;
        }

        String moduleFullName = extractFullModuleName(project, androidModule.getModuleName());
        String[] moduleArray = moduleFullName.split(":");
        String group = "";
        String moduleName = "";
        if (moduleArray.length > 2) {
            group = moduleArray[1];
            moduleName = moduleArray[2];
        } else {
            moduleName = moduleArray[1];
        }

        return new BuildCommand(getProductFlavors(androidModule),
                getBuildType(androidModule),
                moduleFullName + ":" + getAssembleTaskName(androidModule),
                group,
                moduleName
        );
    }

    public static String getAssembleTaskName(AndroidModuleModel androidModule) {
        if (getIdeMajorVersion() >= 2020 && getIdeManorVersion() >= 3) {
            IdeVariant variant = androidModule.getSelectedVariant();

            return variant.getMainArtifact().getAssembleTaskName();
        } else {
            try {
                Method method1 = androidModule.getClass().getMethod("getSelectedVariant");
                Object variant = method1.invoke(androidModule);
                Method method2 = variant.getClass().getMethod("getMainArtifact");
                Object mainArtifact = method2.invoke(variant);
                Method method3 = mainArtifact.getClass().getMethod("getAssembleTaskName");

                return (String) method3.invoke(mainArtifact);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static String getBuildType(AndroidModuleModel androidModule) {
        if (getIdeMajorVersion() >= 2020 && getIdeManorVersion() >= 3) {
            IdeVariant variant = androidModule.getSelectedVariant();

            return variant.getBuildType();
        } else {
            try {
                Method method1 = androidModule.getClass().getMethod("getSelectedVariant");
                Object variant = method1.invoke(androidModule);
                Method method2 = variant.getClass().getMethod("getBuildType");

                return (String) method2.invoke(variant);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static List<String> getProductFlavors(AndroidModuleModel androidModule) {
        if (getIdeMajorVersion() >= 2020 && getIdeManorVersion() >= 3) {
            IdeVariant variant = androidModule.getSelectedVariant();
            return variant.getProductFlavors();
        } else {
            try {
                Method method1 = androidModule.getClass().getMethod("getSelectedVariant");
                Object variant = method1.invoke(androidModule);
                Method method2 = variant.getClass().getMethod("getProductFlavors");

                return (List<String>) method2.invoke(variant);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return null;
    }


    private static String extractFullModuleName(Project project, String moduleName) {
        return moduleName.replace(project.getName(), "").replace(".", ":");
    }

    public static String getPluginVersion() {
        @Nullable IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(PluginId.getId("io.buildman.Buildman"));

        return plugin != null ? plugin.getVersion() : "Unknown";
    }

    public static int getIdeMajorVersion() {
        return Integer.parseInt(ApplicationInfo.getInstance().getMajorVersion());
    }

    public static int getIdeManorVersion() {
        return Integer.parseInt(ApplicationInfo.getInstance().getMinorVersionMainPart());
    }

}
