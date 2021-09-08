package io.buildman.configuration;

import com.android.tools.idea.gradle.run.MakeBeforeRunTaskProvider;
import com.android.tools.idea.help.AndroidWebHelpProvider;
import com.android.tools.idea.run.AndroidRunConfiguration;
import com.android.tools.idea.run.AndroidRunConfigurationFactoryBase;
import com.android.tools.idea.run.AndroidRunConfigurationType;
import com.intellij.compiler.options.CompileStepBeforeRun;
import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationTypeBase;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.util.SmartList;
import icons.Icons;
import icons.StudioIcons;
import io.buildman.task.BuildmanBuildTask;
import org.jetbrains.android.util.AndroidBundle;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BuildmanAndroidRunConfigurationType extends ConfigurationTypeBase {
    public static final String ID = "BuildmanAndroidRunConfigurationType";

    public BuildmanAndroidRunConfigurationType() {
        super(ID,
                "Buildman APP",
                "Buildman launch/debug configuration",
                NotNullLazyValue.createValue(() -> Icons.run_icon));

        addFactory(new BuildmanAndroidRunConfigurationFactory());
    }

    @Override
    public String getHelpTopic() {
        return AndroidWebHelpProvider.HELP_PREFIX + "r/studio-ui/rundebugconfig.html";
    }


    public class BuildmanAndroidRunConfigurationFactory extends AndroidRunConfigurationFactoryBase {
        public BuildmanAndroidRunConfigurationFactory() {
            super(BuildmanAndroidRunConfigurationType.this);
        }

        @Override
        @NotNull
        public String getId() {
            // This ID must be non-localized, use a rae string instead of the message bundle string.
            return "Buildman-App";
        }

        @NotNull
        @Override
        public RunConfiguration createTemplateConfiguration(@NotNull Project project) {

            return new BuildmanAndroidRunConfiguration(project, this);
        }


    }

    public static BuildmanAndroidRunConfigurationType getInstance() {
        return ConfigurationTypeUtil.findConfigurationType(BuildmanAndroidRunConfigurationType.class);
    }

    public ConfigurationFactory getFactory() {
        return getConfigurationFactories()[0];
    }

    @Override
    public boolean isDumbAware() {
        return true;
    }
}
