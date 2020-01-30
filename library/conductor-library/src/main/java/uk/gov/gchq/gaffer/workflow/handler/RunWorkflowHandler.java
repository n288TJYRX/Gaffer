package uk.gov.gchq.gaffer.workflow.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.gov.gchq.gaffer.exception.SerialisationException;
import uk.gov.gchq.gaffer.jsonserialisation.JSONSerialiser;
import uk.gov.gchq.gaffer.operation.OperationException;
import uk.gov.gchq.gaffer.store.Context;
import uk.gov.gchq.gaffer.store.Store;
import uk.gov.gchq.gaffer.store.operation.handler.OperationHandler;
import uk.gov.gchq.gaffer.workflow.RunWorkflow;

import java.util.Arrays;
import java.util.HashMap;

import static uk.gov.gchq.gaffer.workflow.util.ConductorEndpoint.*;

public class RunWorkflowHandler implements OperationHandler<RunWorkflow> {
    int WORKFLOW_CHECK_INTERVAL = 1000;
    int WORKFLOW_TIMEOUT = 30000;

    @Override
    public Object doOperation(final RunWorkflow operation, final Context context, final Store store) throws OperationException {

        // Convert the input to JSON
        ObjectMapper mapper = new ObjectMapper();
        String workflowInputJSON = null;
        try {
            workflowInputJSON = mapper.writeValueAsString(operation.getWorkflowInput());
            System.out.println("workflowInputJSON: " + workflowInputJSON);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        // Start the workflow
        String workflowId = http("POST",BASE_URI + WORKFLOW_START_ENDPOINT + "/" + operation.getWorkflowName(), workflowInputJSON);
        System.out.println("Workflow Id: " + workflowId);

        // Get the output of the workflow using the workflow Id
        Object workflowOutput = null;
        HashMap responseMap = new HashMap<>();
        String workflowStatus = "";
        long timeStart = System.currentTimeMillis();
        while (!workflowStatus.equals("COMPLETED") && !workflowStatus.equals("FAILED") && !workflowStatus.equals("TIMED OUT")) {
            responseMap = new HashMap<>();

            // If the workflow hasn't completed after a long while stop checking
            if (System.currentTimeMillis() > timeStart + WORKFLOW_TIMEOUT) {
                throw new OperationException("Workflow timed out");
            }

            // Wait a while before checking the status again
            try {
                Thread.sleep(WORKFLOW_CHECK_INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Get the response and deserialise it
            String response = http("GET",BASE_URI+WORKFLOW_START_ENDPOINT+"/"+workflowId,null);
            System.out.println("Workflow response: " + response);
            try {
                responseMap = JSONSerialiser.deserialise(response,HashMap.class);
//                System.out.println("Workflow response map: " + responseMap.toString());
                workflowStatus = (String) responseMap.get("status");
                System.out.println("Workflow status: " + workflowStatus);
            } catch (SerialisationException e) {
                e.printStackTrace();
            }
        }

        // Get the workflow output
        workflowOutput = responseMap.get("output");
        System.out.println("WorkflowOutput is: " + workflowOutput.toString());
//        HashMap results = (HashMap) workflowOutput.get("output");
//        System.out.println("Results are: " + results.toString());
        String results = null;
        return Arrays.asList(results);
    }
}
