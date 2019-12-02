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
package uk.gov.gchq.gaffer.script.operation.handler;

import uk.gov.gchq.gaffer.operation.OperationException;
import uk.gov.gchq.gaffer.script.operation.RunScript;
import uk.gov.gchq.gaffer.script.operation.container.Container;
import uk.gov.gchq.gaffer.script.operation.platform.ImagePlatform;
import uk.gov.gchq.gaffer.script.operation.platform.LocalDockerPlatform;
import uk.gov.gchq.gaffer.script.operation.provider.GitScriptProvider;
import uk.gov.gchq.gaffer.script.operation.provider.ScriptProvider;
import uk.gov.gchq.gaffer.store.Context;
import uk.gov.gchq.gaffer.store.Store;
import uk.gov.gchq.gaffer.store.operation.handler.OperationHandler;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class RunScriptHandler implements OperationHandler<RunScript> {

    // comment for json injection
    private ImagePlatform imagePlatform = new LocalDockerPlatform();
    private ScriptProvider scriptProvider = new GitScriptProvider();
    private String repoName = "test";
    private String repoURI = "https://github.com/g609bmsma/test";
    private String ip = "127.0.0.1";

    @Override
    public Object doOperation(final RunScript operation, final Context context, final Store store) throws OperationException {

        final String scriptName = operation.getScriptName();
        final Map<String, Object> scriptParameters = operation.getScriptParameters();
        final String currentWorkingDirectory = FileSystems.getDefault().getPath("").toAbsolutePath().toString();
        final String directoryPath = currentWorkingDirectory.concat("/src/main/resources/" + ".ScriptBin");
        final Path absoluteRepoPath = Paths.get(directoryPath, repoName);
        final File directory = new File(directoryPath);
        if (!directory.exists()) {
            directory.mkdir();
        }

        // Pull or Clone the repo with the files
        scriptProvider.getScripts(absoluteRepoPath.toString(), repoURI);
        // Create the container
        final Container container = imagePlatform.createContainer(scriptName, scriptParameters, directoryPath, ip);
        // Run the container and return the result
        return imagePlatform.runContainer(container, operation.getInput());
    }

    private ImagePlatform getImagePlatform() {
        return imagePlatform;
    }

    private void setImagePlatform(final ImagePlatform imagePlatform) {
        this.imagePlatform = imagePlatform;
    }

    private ScriptProvider getScriptProvider() {
        return scriptProvider;
    }

    private void setScriptProvider(final ScriptProvider scriptProvider) {
        this.scriptProvider = scriptProvider;
    }

    private String getRepoName() {
        return repoName;
    }

    private void setRepoName(final String repoName) {
        this.repoName = repoName;
    }

    private String getRepoURI() {
        return repoURI;
    }

    private void setRepoURI(final String repoURI) {
        this.repoURI = repoURI;
    }

    private String getIp() {
        return ip;
    }

    private void setIp(final String ip) {
        this.ip = ip;
    }
}