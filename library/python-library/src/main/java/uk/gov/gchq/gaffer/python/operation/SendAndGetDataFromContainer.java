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
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SendAndGetDataFromContainer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SendAndGetDataFromContainer.class);

    public SendAndGetDataFromContainer() {
    }

    /**
     * @param operation the RunPythonScript operation
     * @param port the port of the docker client where the data will be passed
     * @return Sets up and closes container
     * @throws InterruptedException should this fail, this will be thrown
     * @throws IOException this will be thrown if non-compliant data is sent
     */
    public StringBuilder setUpAndCloseContainer(final RunPythonScript operation, final String port) throws InterruptedException, IOException {
        // Keep trying to connect to container and give the container some time to load up
        boolean failedToConnect = true;
        IOException error = null;
        HttpURLConnection con = null;
        DataInputStream in = null;
        Thread.sleep(1000);
        System.out.println("Attempting to connect with the container...");
        for (int i = 0; i < 100; i++) {
            try {
                URL url = new URL("HTTP","192.168.99.107",80,"/script1");
                con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                Map<String, String> parameters = new HashMap<>();
                parameters.put("param1", "val");
                con.setDoOutput(true);
                System.out.println("URL = " + con.getURL());
                DataOutputStream out = new DataOutputStream(con.getOutputStream());
                System.out.println("Sending the data: " + getParamsString(parameters));
                out.writeBytes(getParamsString(parameters));
                out.flush();
                out.close();
                con.disconnect();

                con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.setDoOutput(true);
                System.out.println("URL = " + con.getURL());
                in = new DataInputStream(con.getInputStream());
                con.disconnect();

//                clientSocket = new Socket("192.168.99.107", 80);
//                WriteDataToContainer.reroute("script1", clientSocket);
//                System.out.println("Connected to container port at {}" + clientSocket.getRemoteSocketAddress());
//                in = WriteDataToContainer.getInputStream(clientSocket);
                System.out.println("Container ready status: {}" + in.readBoolean());
//                WriteDataToContainer.sendData(operation, clientSocket);
                break;
            } catch (final IOException e) {
                System.out.println(e.getMessage());
                error = e;
                TimeUnit.MILLISECONDS.sleep(100);
            }
        }
        System.out.println("clientSocket is: {}" + con);
        System.out.println("In is: {}" + in);
        int incomingDataLength = 0;
        if (con != null && in != null) {
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
            if (error != null) {
                error.printStackTrace();
            }
        } else {
            for (int i = 0; i < incomingDataLength / 65000; i++) {
                dataReceived.append(in.readUTF());
            }
            dataReceived.append(in.readUTF());
            con.disconnect();
        }
        return dataReceived;


    }

    static String getParamsString(Map<String, String> params)
            throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            result.append("&");
        }

        String resultString = result.toString();
        return resultString.length() > 0
                ? resultString.substring(0, resultString.length() - 1)
                : resultString;
    }
}
