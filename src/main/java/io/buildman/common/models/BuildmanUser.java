package io.buildman.common.models;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;

public class BuildmanUser {
    private static String licenseKey;
    private static BuildmanUser myInstance = new BuildmanUser();
    private  final String KEY = "buildman-license-key";
    private String projectId;
    private BuildmanUser() {

    }

    public static BuildmanUser getInstance() {
        if (myInstance == null) {
            myInstance = new BuildmanUser();
        }
        return myInstance;
    }

    public void saveLicenseKey(String licenseKey) {
        CredentialAttributes credentialAttributes = createCredentialAttributes(KEY); // see previous sample
        Credentials credentials = new Credentials("", licenseKey);
        PasswordSafe.getInstance().set(credentialAttributes, credentials);
    }

    public String getLicenseKey() {
        if (licenseKey != null) {
            return licenseKey;
        }

        CredentialAttributes credentialAttributes = createCredentialAttributes(KEY);

        Credentials credentials = PasswordSafe.getInstance().get(credentialAttributes);
        if (credentials != null) {
            licenseKey = credentials.getPasswordAsString();
            return licenseKey;
        }

        return null;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    private CredentialAttributes createCredentialAttributes(String key) {
        return new CredentialAttributes(
                CredentialAttributesKt.generateServiceName("MySystem", key)
        );
    }
}
