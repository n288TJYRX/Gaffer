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
package uk.gov.gchq.gaffer.store.operation.handler;

import com.google.common.collect.ImmutableMap;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.ProgressHandler;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.PortBinding;
import com.spotify.docker.client.messages.ProgressMessage;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;

import uk.gov.gchq.gaffer.operation.OperationException;
import uk.gov.gchq.gaffer.operation.PythonOperation;
import uk.gov.gchq.gaffer.store.Context;
import uk.gov.gchq.gaffer.store.Store;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class PythonOperationHandler implements OperationHandler<PythonOperation> {

    private static Git git;

    private static Git getGit() {
        if (git == null) {
            try {
                git = Git.open(new File(FileSystems.getDefault().getPath(".").toAbsolutePath() + "/core/store/src/main/resources/test"));
            } catch (RepositoryNotFoundException e) {
                try {
                    git = Git.cloneRepository()
                            .setDirectory(new File(FileSystems.getDefault().getPath(".").toAbsolutePath() + "/core/store/src/main/resources/test"))
                            .setURI("https://github.com/g609bmsma/test")
                            .call();
                    System.out.println("git cloned");
                } catch (GitAPIException e1) {
                    e1.printStackTrace();
                    git = null;
                }
            } catch (IOException e) {
                e.printStackTrace();
                git = null;
            }
        }
        return git;
    }

    @Override
    public Object doOperation(final PythonOperation operation, final Context context, final Store store) throws OperationException {
        final Path hostAbsolutePathRoot = FileSystems.getDefault().getPath(".").toAbsolutePath();
        final String hostAbsolutePathContainerResults = hostAbsolutePathRoot + "/core/store/src/main/resources";
        String operationName = "PythonOperation1";

        File dir = new File(hostAbsolutePathContainerResults + "/test");

        try {
            if (getGit() != null) {
                getGit().pull().call();
                System.out.println("git pulled");
            } else {
                Git.cloneRepository().setDirectory(dir).setURI("https://github.com/g609bmsma/test").call();
                System.out.println("git cloned");
            }
        } catch (GitAPIException e) {
            e.printStackTrace();
        }

        File[] sources = new File[3];
        File[] destinations = new File[3];

        File source1 = new File(dir + "/entrypointPythonOperation1.py");
        File dest1 = new File(hostAbsolutePathContainerResults + "/entrypointPythonOperation1.py");

        sources[0] = source1;
        destinations[0] = dest1;

        File source2 = new File(dir + "/PythonOperation1.py");
        File dest2 = new File(hostAbsolutePathContainerResults + "/PythonOperation1.py");

        sources[1] = source2;
        destinations[1] = dest2;

        File source3 = new File(dir + "/pythonOperation1Modules.txt");
        File dest3 = new File(hostAbsolutePathContainerResults + "/pythonOperation1Modules.txt");

        sources[2] = source3;
        destinations[2] = dest3;

        for (int i = 0; i < sources.length; i++) {
            try (FileInputStream fis = new FileInputStream(sources[i]);
                 FileOutputStream fos = new FileOutputStream(destinations[i])) {

                byte[] buffer = new byte[1024];
                int length;

                while ((length = fis.read(buffer)) > 0) {

                    fos.write(buffer, 0, length);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Load the pythonModules.txt file
        String moduleData = "";
        try {
            FileInputStream fis = new FileInputStream(hostAbsolutePathContainerResults + "/pythonOperation1Modules.txt");
            moduleData = IOUtils.toString(fis, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        String[] modules = moduleData.split("\\n");

        // Create Dockerfile data
        String dockerFileData = "FROM python:3\n";
        String addEntrypointFileLine = "ADD entrypoint" + operationName + ".py /\n";
        String addScriptFileLine = "ADD " + operationName + ".py /\n";
        dockerFileData = dockerFileData + addEntrypointFileLine + addScriptFileLine;

        for (String module : modules) {
            String installLine = "RUN pip install " + module + "\n";
            dockerFileData = dockerFileData + installLine;
        }

        String entrypointLine = "ENTRYPOINT [ \"python\", \"./" + "entrypoint" + operationName + ".py" + "\"]";
        dockerFileData = dockerFileData + entrypointLine;

        // Create a new Dockerfile from the pythonModules.txt
        File file = new File(hostAbsolutePathContainerResults + "/Dockerfile" + operationName + ".yml");
        try {
            if(file.createNewFile()) {
                // File created
                System.out.println("Created a new file");
                Files.write(Paths.get(hostAbsolutePathContainerResults + "/Dockerfile" + operationName + ".yml"), dockerFileData.getBytes());
            } else {
                // File already created
                System.out.println("File already created");
            }
        } catch (IOException e) {
            System.out.println("Failed to create Dockerfile");
            e.printStackTrace();
        }

        try {

            // Start the docker client
            System.out.println("Starting the docker client...");
            DockerClient docker = DefaultDockerClient.fromEnv().build();

            // Build an image from the Dockerfile
            System.out.println("Building the image from Dockerfile...");
            final AtomicReference<String> imageIdFromMessage = new AtomicReference<>();
            final String returnedImageId = docker.build(Paths.get(hostAbsolutePathContainerResults),"myimage:latest", "Dockerfile" + operationName + ".yml", new ProgressHandler() {
                @Override
                public void progress(ProgressMessage message) {
                    final String imageId = message.buildImageId();
                    if (imageId != null) {
                        imageIdFromMessage.set(imageId);
                    }
                    System.out.println(message);
                }}, DockerClient.BuildParam.pullNewerImage());

            // Create a containers from the image id with a bind mount to the docker host
            final ContainerConfig containerConfig = ContainerConfig.builder()
                    .hostConfig( HostConfig.builder()
                            .portBindings(ImmutableMap.of("8080/tcp", Collections.singletonList(PortBinding.of("127.0.0" + ".1", "8080")))).build())
                    .image(returnedImageId)
                    .exposedPorts( "8080/tcp" )
                    .cmd("sh", "-c", "while :; do sleep 1; done")
                    .build();
            final ContainerCreation creation = docker.createContainer(containerConfig);
            final String id = creation.id();

            // Start the container
            System.out.println("Starting the docker container...");
            docker.startContainer(id);

            // Keep trying to connect to container and give the container some time to load up
            boolean failedToConnect = true;
            for (int i = 0; i < 10; i++) {
                System.out.println("Attempting to send data...");
                Socket clientSocket;
                try {
                    clientSocket = new Socket("127.0.0.1", 8080);
                    System.out.println("Connected to container port at " + clientSocket.getRemoteSocketAddress());

                    // Send the data
                    System.out.println("Sending data from " + clientSocket.getLocalSocketAddress() + "...");
                    OutputStream outToContainer = clientSocket.getOutputStream();
                    DataOutputStream out = new DataOutputStream(outToContainer);
                    out.writeUTF("Hello from client at " + clientSocket.getLocalSocketAddress());

                    // Get the data from the container
                    System.out.println("Fetching data from container...");
                    InputStream inFromContainer = clientSocket.getInputStream();
                    DataInputStream in = new DataInputStream(inFromContainer);
                    System.out.println("Container says " + in.readUTF());
                    failedToConnect = false;
                    clientSocket.close();
                    System.out.println("Closed the connection.");
                    break;
                } catch (IOException e) {
                    e.printStackTrace();
                    TimeUnit.MILLISECONDS.sleep(50);
                }
            }

            if (failedToConnect) {
                System.out.println("Connection failed, stopping the container...");
                docker.stopContainer(id,1); // Kill the container after 1 second
            }

            // Delete the container
//            System.out.println("Deleting the container...");
//            docker.removeContainer(id);

            // Close the docker client
            System.out.println("Closing the docker client...");
            docker.close();
            System.out.println("Closed the docker client.");

        } catch (DockerCertificateException | InterruptedException | DockerException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }


}












