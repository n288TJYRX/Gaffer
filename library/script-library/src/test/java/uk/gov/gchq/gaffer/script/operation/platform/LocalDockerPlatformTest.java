/*
 * Copyright 2019 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.gov.gchq.gaffer.script.operation.platform;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import uk.gov.gchq.gaffer.script.operation.DockerFileUtils;
import uk.gov.gchq.gaffer.script.operation.ScriptTestConstants;
import uk.gov.gchq.gaffer.script.operation.builder.DockerImageBuilder;
import uk.gov.gchq.gaffer.script.operation.container.Container;
import uk.gov.gchq.gaffer.script.operation.container.LocalDockerContainer;
import uk.gov.gchq.gaffer.script.operation.image.Image;
import uk.gov.gchq.gaffer.script.operation.provider.GitScriptProvider;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class LocalDockerPlatformTest {

    @Before
    public void setup() {
        GitScriptProvider scriptProvider = new GitScriptProvider();
        final String currentWorkingDirectory = FileSystems.getDefault().getPath("").toAbsolutePath().toString();
        final String directoryPath = currentWorkingDirectory.concat(ScriptTestConstants.CURRENT_WORKING_DIRECTORY);
        Path pathAbsoluteScriptRepo = DockerFileUtils.getPathAbsoluteScriptRepo(directoryPath, ScriptTestConstants.REPO_NAME);
        scriptProvider.getScripts(pathAbsoluteScriptRepo.toString(), ScriptTestConstants.REPO_URI);
    }

    @Test
    public void shouldCreateAContainer() {
        // Given
        LocalDockerPlatform platform = new LocalDockerPlatform();
        final String currentWorkingDirectory = FileSystems.getDefault().getPath("").toAbsolutePath().toString();
        final String directoryPath = currentWorkingDirectory.concat("/src/main/resources/" + ".ScriptBin");
        DockerImageBuilder imageBuilder = new DockerImageBuilder();
        imageBuilder.getFiles(directoryPath, "");
        DockerClient docker = null;
        try {
            docker = DefaultDockerClient.fromEnv().build();
        } catch (DockerCertificateException e) {
            e.printStackTrace();
        }
        Image image = platform.buildImage(ScriptTestConstants.SCRIPT_NAME, null, directoryPath);

        // When
        LocalDockerContainer container = (LocalDockerContainer) platform.createContainer(image, ScriptTestConstants.LOCALHOST);

        try {
            if (docker != null) {
                docker.removeContainer(container.getContainerId());
            }
        } catch (DockerException | InterruptedException e) {
            e.printStackTrace();
        }

        // Then
        Assert.assertTrue(container instanceof LocalDockerContainer);
        Assert.assertNotNull(container);
    }

    @Test
    public void shouldRunTheContainer() {
        // Given
        LocalDockerPlatform platform = new LocalDockerPlatform();
        final String currentWorkingDirectory = FileSystems.getDefault().getPath("").toAbsolutePath().toString();
        final String directoryPath = currentWorkingDirectory.concat("/src/main/resources/" + ".ScriptBin");
        DockerImageBuilder imageBuilder = new DockerImageBuilder();
        imageBuilder.getFiles(directoryPath, "");
        Image image = platform.buildImage(ScriptTestConstants.SCRIPT_NAME, null, directoryPath);
        Container container = platform.createContainer(image, ScriptTestConstants.LOCALHOST);
        List data = new ArrayList();
        data.add("testData");

        // When
        StringBuilder result = platform.runContainer(container, data);

        // Then
        Assert.assertEquals("[\"testData\"]", result.toString());
    }

    @Test
    public void shouldCreateContainer() {
        // Given
        setupTestServer();

        LocalDockerContainer localDockerContainer = new LocalDockerContainer("", ScriptTestConstants.TEST_SERVER_PORT_3);
        ArrayList<String> inputData = new ArrayList<>();
        inputData.add("Test Data");

        // When
        StringBuilder result = null;
        try {
            localDockerContainer.sendData(inputData);
            result = localDockerContainer.receiveData();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Then
        assert result != null;
        Assert.assertEquals("Test Complete", result.toString());
    }

    private void setupTestServer() {
        Runnable serverTask = () -> {
            try (ServerSocket serverSocket = new ServerSocket(ScriptTestConstants.TEST_SERVER_PORT_3)) {
                System.out.println("Waiting for clients to connect...");
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected.");
                DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
                DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
                dos.writeBoolean(true);
                dos.flush();
                dis.readUTF();
                dis.readUTF();
                dos.writeInt(1);
                dos.writeUTF("Test Complete");
                serverSocket.close();
                System.out.println("Closing Socket.");
                dos.flush();
            } catch (IOException e) {
                System.err.println("Unable to process client request");
                System.out.println("Unable to process client request");
                e.printStackTrace();
            }
        };
        final Thread serverThread = new Thread(serverTask);
        serverThread.start();
    }
}
