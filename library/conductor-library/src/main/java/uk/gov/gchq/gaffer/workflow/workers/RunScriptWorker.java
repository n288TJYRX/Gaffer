package uk.gov.gchq.gaffer.workflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.gaffer.script.operation.handler.RunScriptHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static uk.gov.gchq.gaffer.workflow.Register.loadJSON;

public class RunScriptWorker implements Worker {

    private static final Logger LOGGER = LoggerFactory.getLogger(RunScriptWorker.class);

    /** The task definition name */
    private String taskDefName;

    /**
     * Instantiates a new worker.
     *
     * @param taskDefName the task def name
     */
    public RunScriptWorker(String taskDefName) {
        this.taskDefName = taskDefName;
    }

    @Override
    public String getTaskDefName() {
        return taskDefName;
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("Executing {}." + taskDefName);

        TaskResult result = new TaskResult(task);

        // Do the calculations
        try {
            processTask(task, result);
            result.setStatus(TaskResult.Status.COMPLETED);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to execute task: " + task.getTaskDefName());
            result.setStatus(TaskResult.Status.FAILED);
        }

        return result;
    }

    /**
     * Process Task
     *
     * @param task the task called from Conductor
     * @param result the result to return to Conductor
     */
    private void processTask(Task task, TaskResult result) throws Exception {

        System.out.println("Processing task: " + task.getTaskDefName());

        // Get the input to the task
        Map<String, Object> inputMap = task.getInputData();
        System.out.println("inputMap: " + inputMap.toString());

        ArrayList<String> scriptInput = (ArrayList<String>) inputMap.get("scriptInput");
        System.out.println("scriptInput: " + scriptInput);

        HashMap<String, Object> scriptParameters = (HashMap<String, Object>) inputMap.get("scriptParameters");
        System.out.println("ScriptParameters: " + scriptParameters);

        // Run the task
        System.out.println("Running task: " + task.getTaskDefName());

        // Use the handler to run the operation
        Object output = null;
        RunScriptHandler handler = new RunScriptHandler();
        handler.setRepoName("test");
        handler.setRepoURI("https://github.com/g609bmsma/test");
        output = handler.run("script1", scriptInput, scriptParameters);
        System.out.println("Output: " + output);

        // Set the output of this task
        result.getOutputData().put("taskOutput", output);
    }
}
