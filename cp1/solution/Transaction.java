package cp1.solution;

import cp1.base.Resource;
import cp1.base.ResourceOperation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class Transaction {

    private boolean isAborted;
    private boolean isActive;
    private final Collection<Resource> acquiredResources;
    private final List<Resource> operatedResources;
    private final List<ResourceOperation> finishedOperations;

    public Transaction() {
        this.isActive = true;
        this.isAborted = false;
        this.acquiredResources = new HashSet<>();
        this.operatedResources = new ArrayList<>();
        this.finishedOperations = new ArrayList<>();
    }

    public boolean isAborted() {
        return isAborted;
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean isAccessAcquiredForResource(Resource resource) {
        return acquiredResources.contains(resource);
    }

    public Collection<Resource> getAcquiredResources() {
        return acquiredResources;
    }

    public void newAcquiredResource(Resource resource) {
        acquiredResources.add(resource);
    }

    public void finishedOperationOnTheResource(Resource resource, ResourceOperation resourceOperation) {
        operatedResources.add(resource);
        finishedOperations.add(resourceOperation);
    }

    public List<Resource> getOperatedResources() {
        return operatedResources;
    }

    public List<ResourceOperation> getFinishedOperations() {
        return finishedOperations;
    }
}
