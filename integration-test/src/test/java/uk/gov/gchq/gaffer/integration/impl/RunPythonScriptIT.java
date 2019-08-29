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
package uk.gov.gchq.gaffer.integration.impl;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import uk.gov.gchq.gaffer.data.element.Element;
import uk.gov.gchq.gaffer.integration.AbstractStoreIT;
import uk.gov.gchq.gaffer.operation.OperationChain;
import uk.gov.gchq.gaffer.operation.OperationException;
import uk.gov.gchq.gaffer.operation.impl.Limit;
import uk.gov.gchq.gaffer.operation.impl.get.GetAllElements;
import uk.gov.gchq.gaffer.python.operation.RunPythonScript;
import uk.gov.gchq.gaffer.python.operation.ScriptInputType;
import uk.gov.gchq.gaffer.python.operation.ScriptOutputType;
import uk.gov.gchq.gaffer.user.User;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RunPythonScriptIT extends AbstractStoreIT {

    @BeforeClass
    public static void setUp() {
    }

    @Before
    public void before() {
    }

    @Test
    public void shouldCloneTheRepoIfNoLocalRepoExists() {
    }

    @Test
    public void shouldPullTheRepoIfLocalRepoExists() {
    }

    @Test
    public void shouldBuildADockerImage() {
    }








    @Test
    public void shouldOutputElementsIfScriptInputTypeDataFrameAndScriptOutputTypeElements() {
    }

    @Test
    public void shouldOutputElementsIfScriptInputTypeJSONAndScriptOutputTypeElements() throws OperationException, IOException {

        final User user = new User("user01");

        final String scriptName = "script1";
        final Map<String, Object> scriptParameters = new HashMap<String, Object>() {{
            put("a", "b");
        }};
        final String repoName = "test";
        final String repoURI = "https://github.com/g609bmsma/test";
        final String ip = "127.0.0.1";
        final ScriptOutputType scriptOutputType = ScriptOutputType.ELEMENTS;
        final ScriptInputType scriptInputType = ScriptInputType.JSON;

        final GetAllElements getAllElements =
                new GetAllElements.Builder().build();

        final RunPythonScript<Element, Iterable<? extends String>> runPythonScript =
                new RunPythonScript.Builder<Element, Iterable<? extends String>>()
                        .scriptName(scriptName)
                        .scriptParameters(scriptParameters)
                        .repoName(repoName)
                        .repoURI(repoURI)
                        .ip(ip)
                        .scriptOutputType(scriptOutputType)
                        .scriptInputType(scriptInputType)
                        .build();

        OperationChain<Iterable<? extends String>> opChain =
                new OperationChain.Builder()
                        .first(getAllElements)
                        .then(new Limit.Builder<Element>().resultLimit(100).truncate(true).build())
                        .then(runPythonScript)
                        .build();

        final Iterable<? extends String> results = graph.execute(opChain, user);

        System.out.println("results are: " + results);
    }

    @Test
    public void shouldOutputJSONIfScriptInputTypeDataFrameAndScriptOutputTypeJSON() {
    }

    @Test
    public void shouldOutputJSONIfScriptInputTypeJSONAndScriptOutputTypeJSON() {
    }

}
