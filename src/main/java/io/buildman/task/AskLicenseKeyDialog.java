package io.buildman.task;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class AskLicenseKeyDialog extends DialogWrapper {
    public AskLicenseKey askLicenseKey;
    public boolean trialClicked;

    public AskLicenseKeyDialog() {
        super(true);
        setTitle("Buildman License Key");
        init();
    }

    @Override
    protected @Nullable
    JComponent createCenterPanel() {
        askLicenseKey = new AskLicenseKey();
        askLicenseKey.setonTrialClickListener(() -> {
            trialClicked = true;
            performOKAction();
        });

        return askLicenseKey.panel;
    }

    @Override
    public @Nullable
    JComponent getPreferredFocusedComponent() {
        return askLicenseKey.licenseKey;
    }


}

