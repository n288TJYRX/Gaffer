package workflow.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.task.WorkflowTaskCoordinator;
import com.netflix.conductor.client.worker.Worker;

import uk.gov.gchq.gaffer.commonutil.iterable.CloseableIterable;
import uk.gov.gchq.gaffer.data.element.Element;
import uk.gov.gchq.gaffer.store.Context;
import uk.gov.gchq.gaffer.store.Store;
import workflow.RunWorkflow;
import workflow.workers.RunScriptWorker;

import static workflow.util.ConductorEndpoint.*;

public class RunWorkflowHandler {



    public CloseableIterable<? extends Element> doOperation(final RunWorkflow operation, final Context context, final Store store) {

        // Convert the input to JSON
        ObjectMapper mapper = new ObjectMapper();
        String workflowInputJSON = null;
        try {
            workflowInputJSON = mapper.writeValueAsString(operation.getInput());
            System.out.println("workflowInputJSON: " + workflowInputJSON);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        // Start the workflow
        executePost( BASE_URI + WORKFLOW_START_ENDPOINT + "/" + operation.getWorkflowName(), workflowInputJSON);

        // Start the worker
        TaskClient taskClient = new TaskClient();
        taskClient.setRootURI(BASE_URI);
        int threadCount = 2;
        Worker runScriptWorker = new RunScriptWorker("runScriptTask");
        WorkflowTaskCoordinator.Builder builder = new WorkflowTaskCoordinator.Builder();
        WorkflowTaskCoordinator coordinator = builder
                .withWorkers(runScriptWorker)
                .withThreadCount(threadCount)
                .withTaskClient(taskClient)
                .build();
        // Start polling for tasks
        System.out.println("Initiating Worker Manager...");
        coordinator.init();

        // return workflow output somehow?
        return null;
    }
}
