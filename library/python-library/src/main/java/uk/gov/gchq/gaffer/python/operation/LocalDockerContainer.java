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

package uk.gov.gchq.gaffer.python.operation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.gchq.gaffer.jsonserialisation.JSONSerialiser;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class LocalDockerContainer implements Container {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalDockerContainer.class);
    public final String containerId;
    private Socket clientSocket = null;

    public LocalDockerContainer(String containerId) {
        this.containerId = containerId;
    }

    @Override
    public void start(ImagePlatform docker) {
        docker.startContainer(this);
    }

    // data will be operation.getInput()
    @Override
    public void sendData(ArrayList data, String port) {
        LOGGER.info("Attempting to connect with the container...");
        sleep(PythonOperationConstants.ONESECOND);
        // The container will need some time to start up, so keep trying to connect and check
        // that its ready to receive data.
        for (int i = 0; i < PythonOperationConstants.MAXTRIES; i++) {
            try {
                // Connect to the container
                clientSocket = new Socket(PythonOperationConstants.LOCALHOST, Integer.parseInt(port));
                LOGGER.info("Connected to container port at {}", clientSocket.getRemoteSocketAddress());

                // Check the container is ready
                DataInputStream inputStream = getInputStream(clientSocket);
                LOGGER.info("Container ready status: {}", inputStream.readBoolean());
                inputStream.close();

                // Send the data
                OutputStream outToContainer = clientSocket.getOutputStream();
                DataOutputStream outputStream = new DataOutputStream(outToContainer);
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
            } catch (IOException e) {
                LOGGER.info(e.getMessage());
                sleep(PythonOperationConstants.TIMEOUT_100);
            }
        }
    }

    @Override
    public StringBuilder receiveData() {
        // First get the length of the data coming from the container. Keep trying until the container is ready.
        DataInputStream inputStream = null;
        try {
            inputStream = getInputStream(clientSocket);
        } catch (IOException e) {
            LOGGER.info(e.getMessage());
        }
        LOGGER.info("Inputstream is: {}", inputStream);
        int incomingDataLength = 0;
        Boolean failedToConnect = true;
        Exception error = null;
        if (clientSocket != null && inputStream != null) {
            int tries = 0;
            while (tries < PythonOperationConstants.TIMEOUT_100) {
                try {
                    incomingDataLength = inputStream.readInt();
                    LOGGER.info("Length of container...{}", incomingDataLength);
                    failedToConnect = false;
                    break;
                } catch (final IOException e) {
                    tries += 1;
                    error = e;
                    sleep(PythonOperationConstants.TIMEOUT_200);
                }
            }
        }

        // If it failed to get the length of the incoming data then show the error, otherwise return the data.
        StringBuilder dataReceived = new StringBuilder();
        if (failedToConnect) {
            LOGGER.info("Connection failed, stopping the container...");
            if (null != error) {
                error.printStackTrace();
            }
        } else {
            try {
                for (int i = 0; i < incomingDataLength / PythonOperationConstants.MAXBYTES; i++) {
                    dataReceived.append(inputStream.readUTF());
                }
                dataReceived.append(inputStream.readUTF());
                // Show the error message if the script failed
                if (dataReceived.subSequence(0,5) == "Error") {
                    LOGGER.info(dataReceived.subSequence(5,dataReceived.length()).toString());
                }
            } catch (IOException e) {
                LOGGER.info(e.getMessage());
            }
        }
        return dataReceived;
    }

    @Override
    public void close() {
        // Close the socket
        if (clientSocket != null) {
            try {
                clientSocket.close();
            } catch (IOException e) {
                LOGGER.info(e.getMessage());
            }
        }
    }

    private DataInputStream getInputStream(final Socket clientSocket) throws IOException {
        return new DataInputStream(clientSocket.getInputStream());
    }

    private void sleep(final Integer time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            LOGGER.info(e.getMessage());
        }
    }
}
