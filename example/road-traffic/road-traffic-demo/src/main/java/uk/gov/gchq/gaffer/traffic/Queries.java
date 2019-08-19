/*
 * Copyright 2018-2019 Crown Copyright
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
package uk.gov.gchq.gaffer.traffic;

import org.apache.commons.io.IOUtils;

import uk.gov.gchq.gaffer.commonutil.StreamUtil;
import uk.gov.gchq.gaffer.data.element.Element;
import uk.gov.gchq.gaffer.data.element.Entity;
import uk.gov.gchq.gaffer.data.element.comparison.ElementPropertyComparator;
import uk.gov.gchq.gaffer.data.element.function.ElementFilter;
import uk.gov.gchq.gaffer.data.element.function.ElementTransformer;
import uk.gov.gchq.gaffer.data.elementdefinition.view.GlobalViewElementDefinition;
import uk.gov.gchq.gaffer.data.elementdefinition.view.View;
import uk.gov.gchq.gaffer.data.elementdefinition.view.ViewElementDefinition;
import uk.gov.gchq.gaffer.data.generator.CsvGenerator;
import uk.gov.gchq.gaffer.graph.Graph;
import uk.gov.gchq.gaffer.graph.Graph.Builder;
import uk.gov.gchq.gaffer.graph.GraphConfig;
import uk.gov.gchq.gaffer.operation.OperationChain;
import uk.gov.gchq.gaffer.operation.OperationException;
import uk.gov.gchq.gaffer.operation.RunPythonScript;
import uk.gov.gchq.gaffer.operation.data.EntitySeed;
import uk.gov.gchq.gaffer.operation.graph.SeededGraphFilters;
import uk.gov.gchq.gaffer.operation.impl.Limit;
import uk.gov.gchq.gaffer.operation.impl.add.AddElements;
import uk.gov.gchq.gaffer.operation.impl.compare.Sort;
import uk.gov.gchq.gaffer.operation.impl.generate.GenerateElements;
import uk.gov.gchq.gaffer.operation.impl.get.GetAdjacentIds;
import uk.gov.gchq.gaffer.operation.impl.get.GetAllElements;
import uk.gov.gchq.gaffer.operation.impl.get.GetElements;
import uk.gov.gchq.gaffer.operation.impl.output.ToCsv;
import uk.gov.gchq.gaffer.operation.impl.output.ToSet;
import uk.gov.gchq.gaffer.traffic.generator.RoadTrafficStringElementGenerator;
import uk.gov.gchq.gaffer.types.function.FreqMapExtractor;
import uk.gov.gchq.gaffer.user.User;
import uk.gov.gchq.koryphe.impl.predicate.IsMoreThan;
import uk.gov.gchq.koryphe.impl.predicate.range.InDateRangeDual;
import uk.gov.gchq.koryphe.predicate.PredicateMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * This class runs simple java queries against the road traffic graph.
 */
public class Queries {
    public static void main(final String[] args) throws OperationException, IOException, InterruptedException {
        new Queries().run();
    }

    private void run() throws OperationException, IOException, InterruptedException {
        final User user = new User("user01");
        final Graph graph = createGraph(user);

//        pythonPerformanceTest(graph, user);
        runPython(graph, user);
//        parallelTest(graph, user);
        // Get the schema
        //System.out.println(graph.getSchema().toString());

        // Full example
        //runFullExample(graph, user);
    }

    private void parallelTest(final Graph graph, final User user) throws OperationException, InterruptedException {
        //create a callable for each method
        Callable<Void> callable = new Callable<Void>()
        {
            @Override
            public Void call() throws Exception {
                runPython(graph, user);
                return null;
            }

        };

        Callable<Void> callable2 = new Callable<Void>()
        {
            @Override
            public Void call() throws Exception {
                runPython2(graph, user);
                return null;
            }

        };

        List<Callable<Void>> taskList = new ArrayList<Callable<Void>>();
        for (int i = 0; i < 5; i++) {
            taskList.add(callable);
        }
        taskList.add(callable2);
        for (int i = 0; i < 5; i++) {
            taskList.add(callable);
        }

        //create a pool executor with 3 threads
        ExecutorService executor = Executors.newFixedThreadPool(20);

        try {
            //start the threads and wait for them to finish
            executor.invokeAll(taskList);
        } catch (final InterruptedException ie) {
            //do something if you care about interruption;
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        } catch (final InterruptedException e) {
            executor.shutdownNow();
        }
    }

    private void pythonPerformanceTest(final Graph graph, final User user) throws OperationException, InterruptedException {

        //create a callable for each method
        Callable<Void> callable = new Callable<Void>()
        {
            @Override
            public Void call() throws Exception {
                runPython(graph, user);
                return null;
            }

        };

        List<Callable<Void>> taskList = new ArrayList<Callable<Void>>();
        for (int i = 0; i < 10; i++) {
            taskList.add(callable);
        }

        //create a pool executor with 3 threads
        ExecutorService executor = Executors.newFixedThreadPool(20);

        try {
            //start the threads and wait for them to finish
            executor.invokeAll(taskList);
        } catch (final InterruptedException ie) {
            //do something if you care about interruption;
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        } catch (final InterruptedException e) {
            executor.shutdownNow();
        }
    }

    private void runPython(final Graph graph, final User user) throws OperationException {

        final String scriptName = "script1";
        final Map<String, Object> parameters = new HashMap<String, Object>() {{
            put("a", "b");
        }};
        final String repoName = "test";
        final String repoURI = "https://github.com/g609bmsma/test";
        final String ip = "127.0.0.1";

        final GetAllElements getAllElements =
                new GetAllElements.Builder().build();

        final RunPythonScript<Element, Entity> runPythonScript =
                new RunPythonScript.Builder<Element, Entity>()
                        .name(scriptName)
                        .parameters(parameters)
                        .repoName(repoName)
                        .repoURI(repoURI)
                        .ip(ip)
                        .build();

        OperationChain<Entity> opChain =
                new OperationChain.Builder()
                        .first(getAllElements)
                        //.then(new Limit.Builder<Element>().resultLimit(300).build())
                        .then(runPythonScript)
                        .build();

        graph.execute(opChain, user);
    }

    private void runPython2(final Graph graph, final User user) throws OperationException {

        final String scriptName = "script2";
        final Map<String, Object> parameters = new HashMap<String, Object>() {{
            put("a", "b");
        }};

        final GetAllElements getAllElements =
                new GetAllElements.Builder().build();

        final RunPythonScript<Element, Entity> runPythonScript =
                new RunPythonScript.Builder<Element, Entity>()
                        .name(scriptName)
                        .parameters(parameters)
                        .build();

        OperationChain<Entity> opChain =
                new OperationChain.Builder()
                        .first(getAllElements)
                        .then(new Limit.Builder<Element>().resultLimit(5).build())
                        .then(runPythonScript)
                        .build();

        graph.execute(opChain, user);
    }

    private void runFullExample(final Graph graph, final User user) throws OperationException {
        final OperationChain<Iterable<? extends String>> opChain = new OperationChain.Builder()
                .first(new GetAdjacentIds.Builder()
                        .input(new EntitySeed("South West"))
                        .view(new View.Builder()
                                .edge("RegionContainsLocation")
                                .build())
                        .build())
                .then(new GetAdjacentIds.Builder()
                        .view(new View.Builder()
                                .edge("LocationContainsRoad")
                                .build())
                        .build())
                .then(new ToSet<>())
                .then(new GetAdjacentIds.Builder()
                        .view(new View.Builder()
                                .edge("RoadHasJunction")
                                .build())
                        .build())
                .then(new GetElements.Builder()
                        .view(new View.Builder()
                                .globalElements(new GlobalViewElementDefinition.Builder()
                                        .groupBy()
                                        .build())
                                .entity("JunctionUse", new ViewElementDefinition.Builder()
                                        .preAggregationFilter(new ElementFilter.Builder()
                                                .select("startDate", "endDate")
                                                .execute(new InDateRangeDual.Builder()
                                                        .start("2000/01/01")
                                                        .end("2001/01/01")
                                                        .build())
                                                .build())
                                        .postAggregationFilter(new ElementFilter.Builder()
                                                .select("countByVehicleType")
                                                .execute(new PredicateMap<>("BUS", new IsMoreThan(1000L)))
                                                .build())

                                        // Extract the bus count out of the frequency map and store in transient property "busCount"
                                        .transientProperty("busCount", Long.class)
                                        .transformer(new ElementTransformer.Builder()
                                                .select("countByVehicleType")
                                                .execute(new FreqMapExtractor("BUS"))
                                                .project("busCount")
                                                .build())
                                        .build())
                                .build())
                        .inOutType(SeededGraphFilters.IncludeIncomingOutgoingType.OUTGOING)
                        .build())
                .then(new Sort.Builder()
                        .comparators(new ElementPropertyComparator.Builder()
                                .groups("JunctionUse")
                                .property("busCount")
                                .reverse(true)
                                .build())
                        .resultLimit(2)
                        .deduplicate(true)
                        .build())
                // Convert the result entities to a simple CSV in format: Junction,busCount.
                .then(new ToCsv.Builder()
                        .generator(new CsvGenerator.Builder()
                                .vertex("Junction")
                                .property("busCount", "Bus Count")
                                .build())
                        .build())
                .build();

        final Iterable<? extends String> results = graph.execute(opChain, user);

        System.out.println("Full example results:");
        for (final String result : results) {
            System.out.println(result);
        }
    }

    private Graph createGraph(final User user) throws IOException, OperationException {
        final Graph graph = new Builder()
                .config(new GraphConfig.Builder()
                        .graphId("roadTraffic")
                        .build())
                .addSchemas(StreamUtil.openStreams(ElementGroup.class, "schema"))
                .storeProperties(StreamUtil.openStream(getClass(), "accumulo/store.properties"))
                .build();

        final OperationChain<Void> populateChain = new OperationChain.Builder()
                .first(new GenerateElements.Builder<String>()
                        .input(IOUtils.readLines(StreamUtil.openStream(getClass(), "roadTrafficSampleData.csv")))
                        .generator(new RoadTrafficStringElementGenerator())
                        .build())
                .then(new AddElements.Builder()
                        .skipInvalidElements(false)
                        .build())
                .build();
        graph.execute(populateChain, user);

        return graph;
    }
}
