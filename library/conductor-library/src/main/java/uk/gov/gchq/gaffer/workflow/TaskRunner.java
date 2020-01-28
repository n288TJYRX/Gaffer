package uk.gov.gchq.gaffer.workflow;

import com.netflix.conductor.client.automator.TaskRunnerConfigurer;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.worker.Worker;
import uk.gov.gchq.gaffer.workflow.workers.RunScriptWorker;

import java.util.ArrayList;

import static uk.gov.gchq.gaffer.workflow.util.ConductorEndpoint.BASE_URI;

public final class TaskRunner {

    private static TaskRunnerConfigurer instance;

    private TaskRunner() { }

    public static TaskRunnerConfigurer getInstance() {
        if (instance == null) {

            // Create the task client
            TaskClient taskClient = new TaskClient();
            taskClient.setRootURI(BASE_URI);

            // Setup the workers
            int threadCount = 2;
            Worker runScriptWorker = new RunScriptWorker("runScriptTask");
            ArrayList<Worker> workers = new ArrayList<>();
            workers.add(runScriptWorker);

            // Setup the task runner
            TaskRunnerConfigurer.Builder builder = new TaskRunnerConfigurer.Builder(taskClient, workers);
            TaskRunnerConfigurer taskRunner = builder
                    .withThreadCount(threadCount)
                    .build();

            instance = taskRunner;
        }
        return instance;
    }
}
