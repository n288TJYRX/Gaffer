package uk.gov.gchq.gaffer.workflow;

public class WorkerStopper {

    public static void main(String[] args) {

        // Stop the workers
        System.out.println("Stopping Workers...");
        TaskRunner.getInstance().shutdown();
    }
}
