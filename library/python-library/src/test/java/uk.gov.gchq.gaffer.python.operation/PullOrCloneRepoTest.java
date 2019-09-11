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
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PullOrCloneRepoTest  {

    @Test
    public void shouldCloneIfNotAlreadyCloned() {
        //Given
        PullOrCloneRepo pOrC = new PullOrCloneRepo();
        Git git = null;
        final Path pathAbsolutePythonRepo = Paths.get(System.getProperty("user.home"),"Documents","/ANALYTIC/Gaffer","/library/python-library/src/main/resources/","test");
        final RunPythonScript<String, String> operation =
                new RunPythonScript.Builder<String, String>()
                        .repoURI("https://github.com/g609bmsma/test")
                        .build();

        //When
        pOrC.pullOrClone(git, pathAbsolutePythonRepo.toString(), operation);
        String[] files = pathAbsolutePythonRepo.toFile().list();

        //Then
        Assert.assertNotNull(files);
    }

    @Test
    public void shouldPullIfAlreadyCloned() {
        //Given
        PullOrCloneRepo pOrC = new PullOrCloneRepo();
        Git git = mock(Git.class);
        final Path pathAbsolutePythonRepo = Paths.get(System.getProperty("user.home"),"Documents","/ANALYTIC/Gaffer","/library/python-library/src/main/resources/","test");
        final RunPythonScript<String, String> operation =
                new RunPythonScript.Builder<String, String>()
                        .repoURI("https://github.com/g609bmsma/test")
                        .build();

        //When
        when(git.pull()).thenThrow(new CanceledException("Pull method called"));
        pOrC.pullOrClone(git, pathAbsolutePythonRepo.toString(), operation);

    }
}