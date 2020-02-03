package uk.gov.gchq.gaffer.workflow;

import uk.gov.gchq.gaffer.operation.OperationException;
import uk.gov.gchq.gaffer.workflow.handler.RunWorkflowHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Demo {

    public static void main(String args[]) {

        // Create the workflow input
        final String workflowName = "runFAASWorkflow";
        final String input = "Test Sentence";

        Map<String, Object> workflowInput = new HashMap<>();
        workflowInput.put("input", input);

        // Create the workflow operation and use the handler to start the operation
        RunWorkflow runWorkflowOperation = new RunWorkflow.Builder<>()
                .workflowName(workflowName)
                .workflowInput(workflowInput)
                .build();
        RunWorkflowHandler workflowHandler = new RunWorkflowHandler();
        try {
            workflowHandler.doOperation(runWorkflowOperation, null, null);
        } catch (OperationException e) {
            e.printStackTrace();
        }
    }
}
