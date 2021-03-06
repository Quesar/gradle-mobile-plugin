/*
 * @(#)KnappsackUtil.java
 *
 * Copyright C.T.Co Ltd, 15/25 Jurkalnes Street, Riga LV-1046, Latvia. All rights reserved.
 */

package lv.ctco.scm.mobile.knappsack;

import lv.ctco.scm.gradle.utils.PropertyUtil;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import org.gradle.api.Project;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

final class KnappsackUtil {

    private static final String PROP_URL = "knappsack.url";
    private static final String PROP_USER_NAME = "knappsack.user.name";
    private static final String PROP_PSWD = "knappsack.user.password";
    private static final String PROP_APP_ID = "knappsack.app.id";
    private static final String PROP_GROUP_ID = "knappsack.group.id";
    private static final String PROP_STORAGE_ID = "knappsack.storage.id";
    private static final String PROP_VERSION = "knappsack.version";
    private static final String PROP_WHATS_NEW = "knappsack.whatsnew";
    private static final String PROP_WHATS_NEW_FILE = "knappsack.whatsnew.file";
    private static final String PROP_ARTIFACT = "knappsack.artifact";

    private KnappsackUtil() {}

    public static KnappsackExtension setupKnappsackExtension(Project project) {
        KnappsackExtension ext = new KnappsackExtension();
        ext.setUrl(getProperty(project, PROP_URL));
        ext.setUserName(getProperty(project, PROP_USER_NAME));
        ext.setPassword(getProperty(project, PROP_PSWD));
        ext.setApplicationId(getProperty(project, PROP_APP_ID));
        ext.setGroupId(getProperty(project, PROP_GROUP_ID));
        ext.setStorageId(getProperty(project, PROP_STORAGE_ID));
        ext.setVersion(getProperty(project, PROP_VERSION));
        ext.setWhatsNew(getProperty(project, PROP_WHATS_NEW));
        ext.setWhatsNewFileName(getProperty(project, PROP_WHATS_NEW_FILE));
        ext.setArtifactFileName(getProperty(project, PROP_ARTIFACT));
        return ext;
    }

    private static String getProperty(Project project, String property) {
        return PropertyUtil.hasProjectProperty(project, property) ? PropertyUtil.getProjectProperty(project, property) : null;
    }

    private static void validateKnappsackExtension(KnappsackExtension ext) throws IOException {
        if (StringUtils.isBlank(ext.getUrl())) {
            failMissingProperty("Knappsack server URL", "knappsack.url", PROP_URL);
        }
        if (StringUtils.isBlank(ext.getUserName())) {
            failMissingProperty("Knappsack user name", "knappsack.userName", PROP_USER_NAME);
        }
        if (StringUtils.isBlank(ext.getPassword())) {
            failMissingProperty("Knappsack user password", "knappsack.password", PROP_PSWD);
        }
        if (StringUtils.isBlank(ext.getApplicationId())) {
            failMissingProperty("Knappsack application ID", "knappsack.applicationId", PROP_APP_ID);
        }
        if (StringUtils.isBlank(ext.getGroupId())) {
            failMissingProperty("Knappsack group ID", "knappsack.groupId", PROP_GROUP_ID);
        }
        if (StringUtils.isBlank(ext.getStorageId())) {
            failMissingProperty("Knappsack storage ID", "knappsack.storageId", PROP_STORAGE_ID);
        }
        // IMPROVE : Read the project version if properties are not available
        if (StringUtils.isBlank(ext.getVersion())) {
            failMissingProperty("Knappsack application version", "knappsack.version", PROP_VERSION);
        }
        // IMPROVE : Perform automatic fallback to a default message containing some Gradle project data
        if (StringUtils.isBlank(ext.getWhatsNew()) && StringUtils.isBlank(ext.getWhatsNewFileName())) {
            failMissingProperty("Recent changes for the version", "knappsack.whatsNew",
                    "${PROP_WHATS_NEW} or ${PROP_WHATS_NEW_FILE}");
        }
        if (StringUtils.isNotBlank(ext.getWhatsNew()) && StringUtils.isNotBlank(ext.getWhatsNewFileName())) {
            throw new IOException("Properties knappsack.whatsNew and knappsack.whatsNewFileName can not be used together");
        }
        if (StringUtils.isBlank(ext.getArtifactFileName())) {
            failMissingProperty("Knappsack artifact file name", "knappsack.artifact", PROP_ARTIFACT);
        }
    }

    private static void failMissingProperty(String name, String conv, String prop) throws IOException {
        throw new IOException(name+" configuration (convention: "+conv+", "+"property: "+prop+") is not specified");
    }

    private static void loadWhatsNewFile(KnappsackExtension ext) throws IOException {
        if (ext.getWhatsNewFile() != null) {
            File whatsNewFile = ext.getWhatsNewFile();
            if (!whatsNewFile.exists()) {
                throw new IOException("What's new file "+whatsNewFile.getAbsolutePath()+" does not exist");
            }
            if (!whatsNewFile.isFile()) {
                throw new IOException("What's new file "+whatsNewFile.getAbsolutePath()+" is not a regular file");
            }
            try {
                ext.setWhatsNew(FileUtils.readFileToString(whatsNewFile, StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new IOException("Unable to read what's new file "+whatsNewFile.getAbsolutePath(), e);
            }
        }
    }

    private static void ensureArtifactExists(File artifactFile) throws IOException {
        if (!artifactFile.exists()) {
            throw new IOException("Artifact file "+artifactFile.getAbsolutePath()+" does not exist!");
        }
        if (!artifactFile.isFile()) {
            throw new IOException("Artifact file "+artifactFile.getAbsolutePath()+" is not a regular file!");
        }
    }

    static void uploadArtifact(KnappsackExtension ext) throws IOException {
        validateKnappsackExtension(ext);
        ensureArtifactExists(ext.getArtifactFile());
        loadWhatsNewFile(ext);
        Knappsack knappsack = new Knappsack(ext.getUrl());
        knappsack.authenticate(ext.getUserName(), ext.getPassword());
        knappsack.uploadArtifact(ext.getApplicationId(), ext.getGroupId(), ext.getStorageId(), ext.getVersion(), ext.getWhatsNew(), ext.getArtifactFile().getAbsolutePath());
    }

}
