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

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class GitScriptProvider implements ScriptProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitScriptProvider.class);

    public synchronized void pullRepo(final Git git, final String pathAbsolutePythonRepo,
                                      final String repoURI) throws GitAPIException {
        LOGGER.info("Repo already cloned, pulling files...");
        git.pull().call();
        LOGGER.info("Pulled the latest files.");
    }

    public synchronized void cloneRepo(final Git git, final String pathAbsolutePythonRepo,
                                       final String repoURI) throws GitAPIException, IOException {
        Git.open(new File(pathAbsolutePythonRepo));
        LOGGER.info("Cloning repo...");
        Git.cloneRepository().setDirectory(new File(pathAbsolutePythonRepo)).setURI(repoURI).call();
    }
}
