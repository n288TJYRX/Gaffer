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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

public class SendAndGetDataFromContainer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SendAndGetDataFromContainer.class);

    public SendAndGetDataFromContainer() {
    }

    /**
     * @param operation the RunPythonScript operation
     * @param rootPort the port of the docker client where the data will be passed
     * @return Sets up and closes container
     * @throws InterruptedException should this fail, this will be thrown
     * @throws IOException this will be thrown if non-compliant data is sent
     */
    public StringBuilder setUpAndCloseContainer(final RunPythonScript operation, final String rootPort) throws InterruptedException, IOException {
        // Keep trying to connect to container and give the container some time to load up
        boolean failedToConnect = true;
        IOException error = null;
        Socket clientRootSocket = null;
        Socket clientScriptSocket = null;
        DataInputStream in = null;
        Thread.sleep(1000);
        System.out.println("Attempting to connect with the container...");
        for (int i = 0; i < 100; i++) {
            try {
                // Connect to the root port
                clientRootSocket = new Socket("192.168.99.107", Integer.parseInt(rootPort));
                System.out.println("Connected to root port at {}" + clientRootSocket.getRemoteSocketAddress());
                // Send the script name
                OutputStream outToContainer = clientRootSocket.getOutputStream();
                DataOutputStream out = new DataOutputStream(outToContainer);
                out.writeUTF("script1");
                out.flush();
                // Get the script port number
                in = WriteDataToContainer.getInputStream(clientRootSocket);
                System.out.println("Reading script port number...");
                String scriptPort = in.readUTF();
                System.out.println("Script port: {}" + scriptPort);
                // Close the root connection and connect to the script port
                clientRootSocket.close();
                clientScriptSocket = new Socket("192.168.99.107", Integer.parseInt(scriptPort));
                System.out.println("Connected to container port at {}" + clientScriptSocket.getRemoteSocketAddress());
                in = WriteDataToContainer.getInputStream(clientScriptSocket);
                System.out.println("Container ready status: {}" + in.readBoolean());
                // Send the data to the script
                WriteDataToContainer.sendData(operation, clientScriptSocket);
                break;
            } catch (final IOException e) {
                System.out.println(e);
                //System.out.println(e.getMessage());
                error = e;
                TimeUnit.MILLISECONDS.sleep(100);
            }
        }
        System.out.println("clientSocket is: {}" + clientScriptSocket);
        System.out.println("In is: {}" + in);
        int incomingDataLength = 0;
        if (clientScriptSocket != null && in != null) {
            int timeout = 0;
            while (timeout < 100) {
                try {
                    // Get the data from the container
                    incomingDataLength = in.readInt();
                    System.out.println("Length of container...{}" + incomingDataLength);
                    failedToConnect = false;
                    break;
                } catch (final IOException e) {
                    timeout += 1;
                    error = e;
                    TimeUnit.MILLISECONDS.sleep(200);
                }
            }
        }
        StringBuilder dataReceived = new StringBuilder();
        if (failedToConnect) {
            System.out.println("Connection failed, stopping the container...");
            error.printStackTrace();
        } else {
            for (int i = 0; i < incomingDataLength / 65000; i++) {
                dataReceived.append(in.readUTF());
            }
            dataReceived.append(in.readUTF());
            clientScriptSocket.close();
        }
        return dataReceived;
    }
}
