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
package uk.gov.gchq.gaffer.operation;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.exception.CloneFailedException;

import uk.gov.gchq.gaffer.operation.io.InputOutput;
import uk.gov.gchq.gaffer.operation.io.MultiInput;
import uk.gov.gchq.gaffer.operation.serialisation.TypeReferenceImpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.Map;

public class PythonOperation<I_ITEM, O> implements
        InputOutput<Iterable<? extends I_ITEM>, O>,
        MultiInput<I_ITEM>,
        Operation {

    private Iterable<? extends I_ITEM> input;
    private Map<String, String> options;

    @Override
    public Iterable<? extends I_ITEM> getInput() {
        return input;
    }

    @Override
    public void setInput(final Iterable<? extends I_ITEM> input) {
        this.input = input;
    }

    @Override
    public TypeReference<O> getOutputTypeReference() {
        return (TypeReference) new TypeReferenceImpl.Object();
    }

    @Override
    public Operation shallowClone() throws CloneFailedException {
        return new PythonOperation<>();
    }

    @Override
    public Map<String, String> getOptions() {
        return options;
    }

    @Override
    public void setOptions(final Map<String, String> options) {
        this.options = options;
    }

}