package uk.gov.gchq.gaffer.workflow;

import uk.gov.gchq.gaffer.workflow.handler.RunWorkflowHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Demo {

    public static void main(String args[]) {

        // Create the workflow input
        final String workflowName = "runScriptWorkflow";
        final ArrayList<String> scriptInput = new ArrayList<>();
        scriptInput.add("1");
        final Map<String, Object> scriptParameters = new HashMap<String, Object>() { {
            put("a", "b");
        } };
        Map<String, Object> workflowInput = new HashMap<>();
        workflowInput.put("scriptInput", scriptInput);
        workflowInput.put("scriptParameters", scriptParameters);

        // Create the workflow operation and use the handler to start the operation
        RunWorkflow runWorkflowOperation = new RunWorkflow.Builder<>()
                .workflowName(workflowName)
                .workflowInput(workflowInput)
                .build();
        RunWorkflowHandler workflowHandler = new RunWorkflowHandler();
        workflowHandler.doOperation(runWorkflowOperation, null, null);
    }
}
