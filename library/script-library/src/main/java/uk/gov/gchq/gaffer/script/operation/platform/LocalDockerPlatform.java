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

import com.google.common.collect.ImmutableMap;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.PortBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.gaffer.jsonserialisation.JSONSerialiser;
import uk.gov.gchq.gaffer.script.operation.builder.DockerImageBuilder;
import uk.gov.gchq.gaffer.script.operation.container.Container;
import uk.gov.gchq.gaffer.script.operation.container.LocalDockerContainer;
import uk.gov.gchq.gaffer.script.operation.generator.RandomPortGenerator;
import uk.gov.gchq.gaffer.script.operation.handler.RunScriptHandler;
import uk.gov.gchq.gaffer.script.operation.image.DockerImage;
import uk.gov.gchq.gaffer.script.operation.image.Image;
import uk.gov.gchq.gaffer.script.operation.util.DockerClientSingleton;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class LocalDockerPlatform implements ImagePlatform {

    private static final Logger LOGGER = LoggerFactory.getLogger(RunScriptHandler.class);
    private static final String LOCALHOST = "127.0.0.1";
    private static final Integer ONE_SECOND = 1000;
    private static final Integer TIMEOUT_100 = 100;
    private static final Integer TIMEOUT_200 = 200;
    private static final Integer MAX_BYTES = 65000;
    private static final Integer MAX_TRIES = 100;

    private DockerClient docker = null;
    private String dockerfilePath = "";
    private int port;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private Socket clientSocket = null;

    /**
     * Builds a docker image
     *
     * @param scriptName             the name of the script being run
     * @param scriptParameters       the parameters of the script being run
     * @param pathToBuildFiles       the path to the directory containing the build files
     * @return the docker image
     */
    public DockerImage buildImage(final String scriptName, final Map<String, Object> scriptParameters, final String pathToBuildFiles) {

        final DockerImageBuilder dockerImageBuilder = new DockerImageBuilder();

        // Get the user defined dockerfile or use the default
        dockerImageBuilder.getFiles(pathToBuildFiles, dockerfilePath);

        // Connect to the Docker client. To ensure only one reference to the Docker client and to avoid
        // memory leaks, synchronize this code amongst multiple threads.
        LOGGER.info("Connecting to the Docker client...");
        docker = DockerClientSingleton.getInstance();
        LOGGER.info("Docker is now: {}", docker);
        final DockerImage dockerImage = (DockerImage) dockerImageBuilder.buildImage(scriptName, scriptParameters, pathToBuildFiles);

        // Remove the old images
        final List<com.spotify.docker.client.messages.Image> images;
        try {
            images = docker.listImages();
            String repoTag = "[<none>:<none>]";
            for (final com.spotify.docker.client.messages.Image image : images) {
                if (Objects.requireNonNull(image.repoTags()).toString().equals(repoTag)) {
                    docker.removeImage(image.id());
                }
            }
        } catch (final DockerException | InterruptedException e) {
            LOGGER.info("Could not remove image, image still in use.");
        }

        return dockerImage;
    }

    /**
     * Builds a docker image and creates a docker container instance.
     *
     * @param image                  the image to create a container from
     * @param ip                     the ip the container is connected to
     * @return the docker container
     */
    @Override
    public Container createContainer(final Image image, final String ip) {

        String containerId = "";
        // Keep trying to create a container and find a free port.
        try {
            port = RandomPortGenerator.getInstance().generatePort();

            // Create a container from the image and bind ports
            final ContainerConfig containerConfig = ContainerConfig.builder().hostConfig(HostConfig.builder().portBindings(ImmutableMap.of("80/tcp", Collections.singletonList(PortBinding.of(ip, port)))).build()).image(image.getImageString()).exposedPorts("80/tcp").cmd("sh", "-c", "while :; do sleep 1; done").build();
            final ContainerCreation creation = docker.createContainer(containerConfig);
            containerId = creation.id();
        } catch (final DockerException | InterruptedException e) {
            e.printStackTrace();
        }
        return new LocalDockerContainer(containerId, port);
    }

    /**
     * Starts a docker container
     *
     * @param container             the container
     */
    private void startContainer(final Container container) {
        for (int i = 0; i < 100; i++) {
            try {
                LOGGER.info("Starting the Docker container...");
                docker.startContainer(container.getContainerId());
                break;
            } catch (final DockerException | InterruptedException ignored) {
            }
        }
    }

    /**
     * Stops and closes a docker container
     *
     * @param container             the container
     */
    private void closeContainer(final Container container) {
        try {
            LOGGER.info("Closing the Docker container...");
            docker.waitContainer(container.getContainerId());
            docker.removeContainer(container.getContainerId());
        } catch (final DockerException | InterruptedException e) {
            e.printStackTrace();
            LOGGER.info("Failed to stop the container");
        }
    }

    /**
     * Runs a docker container
     *
     * @param container              the container to run
     * @param inputData              the data to pass to the container
     * @return the result of the container
     */
    @Override
    public StringBuilder runContainer(final Container container, final Iterable inputData) {
        startContainer(container);
        sendData(inputData);
        StringBuilder dataReceived = receiveData();
        closeContainer(container);
        RandomPortGenerator.getInstance().freePort(port);
        return dataReceived;
    }

    /**
     * Sends data to the docker container
     *
     * @param data             the data being sent
     */
    @Override
    public void sendData(final Iterable data) {
        LOGGER.info("Attempting to connect with the container...");

        sleep(ONE_SECOND);
        // The container will need some time to start up, so keep trying to connect and check
        // that its ready to receive data.
        Exception error = null;
        for (int i = 0; i < MAX_TRIES; i++) {
            try {
                // Connect to the container
                clientSocket = new Socket(LOCALHOST, port);
                LOGGER.info("Connected to container port at {}", clientSocket.getRemoteSocketAddress());

                // Check the container is ready
                inputStream = getInputStream(clientSocket);
                LOGGER.info("Container ready status: {}", inputStream.readBoolean());

                // Send the data
                OutputStream outToContainer = clientSocket.getOutputStream();
                outputStream = new DataOutputStream(outToContainer);
                boolean firstObject = true;
                for (final Object current : data) {
                    if (firstObject) {
                        outputStream.writeUTF("[" + new String(JSONSerialiser.serialise(current)));
                        firstObject = false;
                    } else {
                        outputStream.writeUTF(", " + new String(JSONSerialiser.serialise(current)));
                    }
                }
                outputStream.writeUTF("]");
                LOGGER.info("Sending data to docker container from {}", clientSocket.getLocalSocketAddress() + "...");

                outputStream.flush();
                break;
            } catch (final IOException e) {
                LOGGER.info(e.getMessage());
                error = e;
                sleep(TIMEOUT_100);
            }
        }
        // Only print an error if it still fails after many tries
        if (error != null) {
            error.printStackTrace();
        }
    }

    /**
     * Retrieves data from the docker container
     *
     * @return the data
     */
    @Override
    public StringBuilder receiveData() {
        // First get the length of the data coming from the container. Keep trying until the container is ready.
        LOGGER.info("Inputstream is: {}", inputStream);
        int incomingDataLength = 0;
        Exception error = null;
        if (clientSocket != null && inputStream != null) {
            int tries = 0;
            while (tries < TIMEOUT_100) {
                try {
                    incomingDataLength = inputStream.readInt();
                    LOGGER.info("Length of container...{}", incomingDataLength);
                    error = null;
                    break;
                } catch (final IOException e) {
                    tries += 1;
                    error = e;
                    sleep(TIMEOUT_200);
                }
            }
        }

        // If it failed to get the length of the incoming data then show the error, otherwise return the data.
        StringBuilder dataReceived = new StringBuilder();
        if (null != error) {
            LOGGER.info("Connection failed, stopping the container...");
            error.printStackTrace();
        } else {
            try {
                // Get the data
                for (int i = 0; i < incomingDataLength / MAX_BYTES; i++) {
                    dataReceived.append(inputStream.readUTF());
                }
                dataReceived.append(inputStream.readUTF());
                // Show the error message if the script failed and return no data
                if (dataReceived.subSequence(0, 5) == "Error") {
                    LOGGER.info(dataReceived.subSequence(5, dataReceived.length()).toString());
                    dataReceived = null;
                }
            } catch (final IOException e) {
                LOGGER.info(e.getMessage());
            }
        }
        try {
            if (clientSocket != null) {
                clientSocket.close();
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return dataReceived;
    }

    private DataInputStream getInputStream(final Socket clientSocket) throws IOException {
        return new DataInputStream(clientSocket.getInputStream());
    }

    private void sleep(final Integer time) {
        try {
            Thread.sleep(time);
        } catch (final InterruptedException e) {
            LOGGER.info(e.getMessage());
        }
    }

    private String getDockerfilePath() {
        return dockerfilePath;
    }

    private void setDockerfilePath(final String dockerfilePath) {
        this.dockerfilePath = dockerfilePath;
    }
}
