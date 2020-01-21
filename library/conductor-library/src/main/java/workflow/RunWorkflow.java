package workflow;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.exception.CloneFailedException;
import uk.gov.gchq.gaffer.commonutil.iterable.CloseableIterable;
import uk.gov.gchq.gaffer.operation.Operation;
import uk.gov.gchq.gaffer.operation.io.InputOutput;
import uk.gov.gchq.gaffer.operation.io.MultiInput;
import uk.gov.gchq.gaffer.operation.serialisation.TypeReferenceImpl;

import java.util.Map;

public class RunWorkflow<I_ITEM, O> implements
        InputOutput<Iterable<? extends I_ITEM>, CloseableIterable<? extends O>>,
        MultiInput<I_ITEM>,
        Operation {

    private Iterable<? extends I_ITEM> input;
    private String workflowName;
    private Map<String, Object> workflowParameters;
    private Map<String, String> options;

    public Iterable<? extends I_ITEM> getInput() {
        return input;
    }

    public void setInput(final Iterable<? extends I_ITEM> input) {
        this.input = input;
    }

    public void setWorkflowName(final String workflowName) {
        this.workflowName = workflowName;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public Map<String, Object> getWorkflowParameters() {
        return workflowParameters;
    }

    public void setWorkflowParameters(final Map<String, Object> workflowParameters) {
        this.workflowParameters = workflowParameters;
    }

    @Override
    public TypeReference<CloseableIterable<? extends O>> getOutputTypeReference() {
        return (TypeReference) new TypeReferenceImpl.Object();
    }

    @Override
    public Operation shallowClone() throws CloneFailedException {
        return new RunWorkflow.Builder<>()
                .workflowName(workflowName)
                .workflowParameters(workflowParameters)
                .build();
    }

    @Override
    public Map<String, String> getOptions() {
        return this.options;
    }

    @Override
    public void setOptions(Map<String, String> options) {
        this.options = options;
    }

    public static class Builder<I_ITEM, O> extends BaseBuilder<RunWorkflow<I_ITEM, O>, Builder<I_ITEM, O>>
            implements  InputOutput.Builder<RunWorkflow<I_ITEM, O>,
                            Iterable<? extends I_ITEM>,
                            CloseableIterable<? extends O>, Builder<I_ITEM, O>>,
                        MultiInput.Builder<RunWorkflow<I_ITEM, O>, I_ITEM, Builder<I_ITEM, O>> {

        public Builder() {
            super(new RunWorkflow<>());
        }

        public Builder<I_ITEM, O> workflowName(final String workflowName) {
            _getOp().setWorkflowName(workflowName);
            return _self();
        }

        public Builder<I_ITEM, O> workflowParameters(final Map<String, Object> workflowParameters) {
            _getOp().setWorkflowParameters(workflowParameters);
            return _self();
        }
    }
}