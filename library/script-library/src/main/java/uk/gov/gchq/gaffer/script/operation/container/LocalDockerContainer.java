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

package uk.gov.gchq.gaffer.script.operation.container;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.gaffer.jsonserialisation.JSONSerialiser;

import java.io.*;
import java.net.Socket;

public class LocalDockerContainer implements Container {

    private String containerId;
    private int port;

    public LocalDockerContainer(final String containerId, final int port) {
        this.containerId = containerId;
        this.port = port;
    }

    @Override
    public String getContainerId() {
        return containerId;
    }

    @Override
    public int getPort() {
        return port;
    }

}
