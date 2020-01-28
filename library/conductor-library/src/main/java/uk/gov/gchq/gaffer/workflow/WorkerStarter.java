package uk.gov.gchq.gaffer.workflow;

public class WorkerStarter {

    public static void main(String[] args) {

        // Start the workers
        System.out.println("Starting Workers...");
        TaskRunner.getInstance().init();
    }
}
