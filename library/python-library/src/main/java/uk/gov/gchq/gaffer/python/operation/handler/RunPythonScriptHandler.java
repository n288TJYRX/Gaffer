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
package uk.gov.gchq.gaffer.python.operation.handler;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.exceptions.DockerRequestException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.Image;
import com.spotify.docker.client.messages.PortBinding;
import org.eclipse.jgit.api.Git;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.gaffer.commonutil.iterable.CloseableIterator;
import uk.gov.gchq.gaffer.commonutil.iterable.WrappedCloseableIterable;
import uk.gov.gchq.gaffer.commonutil.iterable.WrappedCloseableIterator;
import uk.gov.gchq.gaffer.data.element.Element;
import uk.gov.gchq.gaffer.jsonserialisation.JSONSerialiser;
import uk.gov.gchq.gaffer.operation.OperationException;
import uk.gov.gchq.gaffer.python.operation.BuildImageFromDockerfile;
import uk.gov.gchq.gaffer.python.operation.GetPort;
import uk.gov.gchq.gaffer.python.operation.PullOrCloneRepo;
import uk.gov.gchq.gaffer.python.operation.RunPythonScript;
import uk.gov.gchq.gaffer.python.operation.ScriptInputType;
import uk.gov.gchq.gaffer.python.operation.ScriptOutputType;
import uk.gov.gchq.gaffer.python.operation.SendAndGetDataFromContainer;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class RunPythonScriptHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RunPythonScriptHandler.class);
    private final SendAndGetDataFromContainer sendAndGetDataFromContainer = new SendAndGetDataFromContainer();
    private final PullOrCloneRepo pullOrCloneRepo = new PullOrCloneRepo();
    private final BuildImageFromDockerfile buildImageFromDockerfile = new BuildImageFromDockerfile();
    private final GetPort getPort = new GetPort();
    private Git git = null;
    private DockerClient docker = null;
    private String containerId = null;

    public Object doOperation(final RunPythonScript operation) throws OperationException {

        final String scriptName = operation.getScriptName();
        final Map<String, Object> scriptParameters = operation.getScriptParameters();
        final String port = "32005";

        StringBuilder dataReceived = null;
        try {
            dataReceived = sendAndGetDataFromContainer.setUpAndCloseContainer(operation, port);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        LOGGER.info("Closed the connection.");
        LOGGER.info("Data recieved is: " + dataReceived.toString());
        return dataReceived;
    }
}
