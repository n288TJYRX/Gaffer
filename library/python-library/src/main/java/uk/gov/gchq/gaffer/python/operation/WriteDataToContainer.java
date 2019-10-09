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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

final class WriteDataToContainer {
    private static final Logger LOGGER = LoggerFactory.getLogger(WriteDataToContainer.class);

    private WriteDataToContainer() {
    }

    /**
     * Sends data to and gets data from container
     * @param operation the RunPythonScript operation
     * @param clientSocket the socket on which data will be sent to the container
     * @throws IOException this will be thrown if non-compliant data is sent
     */
    static void sendData(final RunPythonScript operation, final Socket clientSocket) throws IOException {
        OutputStream outToContainer = clientSocket.getOutputStream();
        DataOutputStream out = new DataOutputStream(outToContainer);
        boolean firstObject = true;
        System.out.println("Operation input is: " + operation.getInput());

        String header = "POST /" + "script1" + " HTTP/1.1\r\n" + "Host:pythonserverproject.192.168.99.107.nip.io\r\n\r\n + -d 'somedata'" ;
        byte[] byteHeader = header.getBytes();
        out.write(byteHeader,0,byteHeader.length);

//        for (final Object current : operation.getInput()) {
//            System.out.println("Current is: " + current);
//            if (firstObject) {
//                out.writeUTF("[" + new String(JSONSerialiser.serialise(current)));
//                firstObject = false;
//            } else {
//                out.writeUTF(", " + new String(JSONSerialiser.serialise(current)));
//            }
//        }
//        out.writeUTF("]");
        System.out.println("Sending data to docker container from {}" + clientSocket.getLocalSocketAddress() + "...");
        out.flush();
    }

    static DataInputStream getInputStream(final Socket clientSocket) throws IOException {
        return new DataInputStream(clientSocket.getInputStream());
    }

    static void reroute(final String path, final Socket clientSocket) throws IOException {
        String header = "POST /" + path + " HTTP/1.1\r\n" + "Host:pythonserverproject.192.168.99.107.nip.io\r\n\r\n";
        OutputStream outToContainer = clientSocket.getOutputStream();
        DataOutputStream out = new DataOutputStream(outToContainer);
        byte[] byteHeader = header.getBytes();
        out.write(byteHeader,0,byteHeader.length);
//        out.writeUTF(header);
        out.flush();
    }
}
