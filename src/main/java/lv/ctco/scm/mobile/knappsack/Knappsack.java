/*
 * @(#)Knappsack.java
 *
 * Copyright C.T.Co Ltd, 15/25 Jurkalnes Street, Riga LV-1046, Latvia. All rights reserved.
 */

package lv.ctco.scm.mobile.knappsack;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This class provides means to connect to a Knappsack server and perform deployment of an
 * artifact. It handles authentication and SSL connections as well. All operations are
 * performed using plain HTTP requests.
 */
class Knappsack {

    private static final Logger logger = Logging.getLogger(Knappsack.class);

    private CloseableHttpClient httpClient;
    private String serverUrl;

    /**
     * Constructor.
     * @param serverUrl Knappsack server URL to connect to.
     */
    Knappsack(String serverUrl) {
        HttpClientBuilder httpClientBuilder = HttpClients.custom();
        this.httpClient = httpClientBuilder.build();
        this.serverUrl = StringUtils.appendIfMissing(serverUrl, "/");
    }

    /**
     * Performs web-based authentication on the server.
     * @param username Username to use for authentication.
     * @param password Password to use for authentication.
     * @throws IOException .
     */
    void authenticate(String username, String password) throws IOException {
        HttpPost httpPost = new HttpPost(serverUrl+"j_spring_security_check");
        List<NameValuePair> postParameters = new ArrayList<>();
        postParameters.add(new BasicNameValuePair("action", "verify"));
        postParameters.add(new BasicNameValuePair("j_username", username));
        postParameters.add(new BasicNameValuePair("j_password", password));
        httpPost.setEntity(new UrlEncodedFormEntity(postParameters));
        String requestLine = httpPost.getRequestLine().toString();
        String postEntity = EntityUtils.toString(httpPost.getEntity()).replaceAll(password, "********");
        logger.info("Executing request {} {}", requestLine, postEntity);
        ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
            @Override
            public String handleResponse(final HttpResponse response) throws IOException {
                int status = response.getStatusLine().getStatusCode();
                String location = response.getFirstHeader("Location").getValue();
                for (Header header : response.getAllHeaders()) {
                    logger.log(LogLevel.DEBUG,"{}", header.toString());
                }
                if (status == 302 && serverUrl.equals(location)) {
                    return null;
                } else {
                    logger.error("Unexpected response status '{}' or redirect '{}' while authenticating", status, location);
                    throw new IOException("Knappsack authentication exception");
                }
            }
        };
        httpClient.execute(httpPost, responseHandler);
    }

    /**
     * Uploads a single artifact (e.g. IPA file) to Knappsack server. Convenience method that accepts full
     * artifact file path, parses its name and opens an InputStream.
     * @param parentId Application id.
     * @param groupId Group id.
     * @param storageConfigurationId Storage configuration id.
     * @param versionName Application version.
     * @param recentChanges Changes in uploaded version in HTML format that will be shown to user.
     * @param filePath File path of the artifact.
     * @throws IOException .
     */
    void uploadArtifact(String parentId, String groupId, String storageConfigurationId, String versionName,
                        String recentChanges, String filePath) throws IOException {
        File file = new File(filePath);
        InputStream stream = new FileInputStream(file);
        logger.info("Uploading artifact '{}' with version '{}' to '{}'", file.getName(), versionName, serverUrl);
        uploadArtifact(parentId, groupId, storageConfigurationId, versionName, recentChanges, stream, file.getName());
    }

    /**
     * Uploads a single artifact (e.t. IPA file) to Knappsack server.
     * @param parentId Application id.
     * @param groupId Group id.
     * @param storageConfigurationId Storage configuration id.
     * @param versionName Application version.
     * @param recentChanges Changes in uploaded version in HTML format that will be shown to user.
     * @param inputStream Input stream to read artifact binary date from.
     * @param fileName File name of the artifact.
     * @throws IOException .
     */
    void uploadArtifact(final String parentId, String groupId, String storageConfigurationId, String versionName,
                        String recentChanges, InputStream inputStream, String fileName) throws IOException {
        HttpPost httpPost = new HttpPost(serverUrl+"manager/uploadVersion");
        MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
        entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        entityBuilder.setBoundary("--gcmp");
        entityBuilder.addTextBody("id", "");
        entityBuilder.addTextBody("parentId", parentId);
        entityBuilder.addTextBody("groupId", groupId);
        entityBuilder.addTextBody("editing", "false");
        entityBuilder.addTextBody("storageConfigurationId", storageConfigurationId);
        entityBuilder.addTextBody("versionName", versionName);
        entityBuilder.addTextBody("recentChanges", recentChanges);
        entityBuilder.addTextBody("_wysihtml5_mode", "1");
        entityBuilder.addBinaryBody("appFile", inputStream, ContentType.DEFAULT_BINARY, fileName);
        entityBuilder.addTextBody("appState", "GROUP_PUBLISH");
        entityBuilder.setContentType(ContentType.MULTIPART_FORM_DATA);
        try {
            httpPost.setEntity(entityBuilder.build());
            logger.info("Executing request "+httpPost.getRequestLine());
            for (Header header : httpPost.getAllHeaders()) {
                logger.log(LogLevel.DEBUG, "{}", header.toString());
            }
            ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
                @Override
                public String handleResponse(final HttpResponse response) throws IOException {
                    int status = response.getStatusLine().getStatusCode();
                    for (Header header : response.getAllHeaders()) {
                        logger.log(LogLevel.DEBUG, "{}", header.toString());
                    }
                    HttpEntity entity = response.getEntity();
                    if (status == 302) {
                        String location = response.getFirstHeader("Location").getValue();
                        if (location.equals(serverUrl+"manager/editApplication/"+parentId)) {
                            return null;
                        } else {
                            throw new IOException("Unexpected redirect'"+location+"' while uploading");
                        }
                    } else if (status == 301) {
                        throw new IOException("App with provided version already exists");
                    } else {
                        logger.log(LogLevel.DEBUG,"{}", EntityUtils.toString(entity));
                        throw new IOException("Unexpected response status: "+status);
                    }
                }
            };
            httpClient.execute(httpPost, responseHandler);
        } finally {
            httpClient.close();
        }
    }

}
