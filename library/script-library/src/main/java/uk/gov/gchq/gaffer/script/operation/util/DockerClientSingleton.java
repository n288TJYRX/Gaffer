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
package uk.gov.gchq.gaffer.script.operation.util;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.gchq.gaffer.script.operation.provider.GitScriptProvider;

public final class DockerClientSingleton {
    private static volatile DockerClient dockerClient;
    private static final Logger LOGGER = LoggerFactory.getLogger(DockerClientSingleton.class);

    private DockerClientSingleton() { }

    public static DockerClient getInstance() {
        // Don't wait for other threads if the instance is available
        if (dockerClient == null) {
            // Synchronize the creation of the docker client
            synchronized (DockerClientSingleton.class) {
                // Creates a docker client if none already exist
                if (dockerClient == null) {
                    try {
                        dockerClient = DefaultDockerClient.fromEnv().build();
                    } catch (final DockerCertificateException e) {
                        LOGGER.error(e.getMessage());
                    }
                }
            }
        }
        return dockerClient;
    }
}
