package uk.gov.gchq.gaffer.workflow;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

import static uk.gov.gchq.gaffer.workflow.util.ConductorEndpoint.*;

public class Register {

    public static void main(String[] args) {

        // Register task definitions
        String task = loadJSON(new File("").getAbsolutePath() + "/library/conductor-library/src/main/resources/taskDefinitions/runScriptTask.json");
        http("POST",BASE_URI + TASK_DEF_ENDPOINT, task);

        // Register workflow definition
        // You may get an error saying the workflow already exists if you've already run this code once
        String workflow = loadJSON(new File("").getAbsolutePath() + "/library/conductor-library/src/main/resources/workflowDefinitions/runScriptWorkflow.json");
        http("POST",BASE_URI + WORKFLOW_DEF_ENDPOINT, workflow);

    }

    public static String loadJSON(String filename) {
        String json = null;
        try {
            json = new String(Files.readAllBytes(Paths.get(filename)));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return json;
    }
}
