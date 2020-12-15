package cp1.solution;

import cp1.base.ResourceId;
import cp1.base.ResourceOperation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Semaphore;

public class Transaction {

    private boolean isAborted;
    private boolean isActive;
    private final Collection<ResourceId> acquiredResources;
    private final List<ResourceId> operatedResources;
    private final List<ResourceOperation> finishedOperations;
    private final long startingTime;
    private final Thread thread;
    private final Semaphore semaphore;

    public Transaction(Thread thread, long startingTime) {
        this.thread = thread;
        this.startingTime = startingTime;
        this.isActive = true;
        this.isAborted = false;
        this.acquiredResources = new HashSet<>();
        this.operatedResources = new ArrayList<>();
        this.finishedOperations = new ArrayList<>();
        this.semaphore = new Semaphore(0);
    }

    public boolean isAborted() {
        return isAborted;
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean wasAccessAcquiredForResource(ResourceId rid) {
        return acquiredResources.contains(rid);
    }

    public Collection<ResourceId> getAcquiredResources() {
        return acquiredResources;
    }

    public void newAcquiredResource(ResourceId rid) {
        acquiredResources.add(rid);
    }

    public void finishedOperationOnTheResource(ResourceId rid, ResourceOperation resourceOperation) {
        operatedResources.add(rid);
        finishedOperations.add(resourceOperation);
    }

    public List<ResourceId> getOperatedResources() {
        return operatedResources;
    }

    public List<ResourceOperation> getFinishedOperations() {
        return finishedOperations;
    }

    public long getStartingTime() {
        return startingTime;
    }

    public Thread getThread() {
        return thread;
    }

    public void abort() {
        isAborted = true;
        isActive = false;
    }

    public Semaphore getSemaphore() {
        return semaphore;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Transaction)) {
            return false;
        }
        return this.thread.getId() == ((Transaction)obj).thread.getId();
    }

    @Override
    public int hashCode() {
        return Long.hashCode(thread.getId());
    }
}
