package cp1.solution;

import cp1.base.ActiveTransactionAborted;
import cp1.base.AnotherTransactionActiveException;
import cp1.base.LocalTimeProvider;
import cp1.base.NoActiveTransactionException;
import cp1.base.Resource;
import cp1.base.ResourceId;
import cp1.base.ResourceOperation;
import cp1.base.ResourceOperationException;
import cp1.base.TransactionManager;
import cp1.base.UnknownResourceIdException;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TransactionManagerImpl implements TransactionManager {

    private final ConcurrentMap<Thread, Transaction> threadTransactionMap;
    private final ConcurrentMap<ResourceId, Resource> resources;
    private final LocalTimeProvider timeProvider;

    private final AllocationGraph resourceAllocationGraph;

    public TransactionManagerImpl(Collection<Resource> resources, LocalTimeProvider localTimeProvider) {
        this.resources = new ConcurrentHashMap<>();
        for (Resource resource : resources) {
            this.resources.put(resource.getId(), resource);
        }
        this.timeProvider = localTimeProvider;
        this.threadTransactionMap = new ConcurrentHashMap<>();
        this.resourceAllocationGraph = new AllocationGraph(resources);
    }

    @Override
    public void startTransaction() throws AnotherTransactionActiveException {
        Thread currentThread = Thread.currentThread();
        if (threadTransactionMap.containsKey(currentThread)) {
            throw new AnotherTransactionActiveException();
        }
        Transaction transaction = new Transaction(currentThread, timeProvider.getTime());
        threadTransactionMap.put(currentThread, transaction);
    }

    @Override
    public void operateOnResourceInCurrentTransaction(ResourceId rid, ResourceOperation operation)
            throws NoActiveTransactionException,
            UnknownResourceIdException,
            ActiveTransactionAborted,
            ResourceOperationException,
            InterruptedException {
        Thread currentThread = Thread.currentThread();
        if (!threadTransactionMap.containsKey(currentThread)) {
            throw new NoActiveTransactionException();
        }
        Transaction transaction = threadTransactionMap.get(currentThread);
        if (!resources.containsKey(rid)) {
            throw new UnknownResourceIdException(rid);
        }
        if (threadTransactionMap.get(currentThread).isAborted()) {
            throw new ActiveTransactionAborted();
        }
        if (!transaction.wasAccessAcquiredForResource(rid)) {
            if (resourceAllocationGraph.addEdgeIfNecessary(transaction, rid)) {
                System.err.println(currentThread.getName() + " is waiting for " + rid);
                transaction.getSemaphore().acquire();
            }
            transaction.newAcquiredResource(rid);
        }
        System.err.println(currentThread.getName() + " is using " + rid);
        resources.get(rid).apply(operation);
        transaction.finishedOperationOnTheResource(rid, operation);
    }

    @Override
    public void commitCurrentTransaction() throws NoActiveTransactionException,
            ActiveTransactionAborted {
        Thread currentThread = Thread.currentThread();
        if (!threadTransactionMap.containsKey(currentThread)) {
            throw new NoActiveTransactionException();
        }
        if (threadTransactionMap.get(currentThread).isAborted()) {
            throw new ActiveTransactionAborted();
        }
        Transaction transaction = threadTransactionMap.get(currentThread);
        resourceAllocationGraph.removeNode(transaction);
        threadTransactionMap.remove(currentThread);
    }

    @Override
    public void rollbackCurrentTransaction() {
        Thread currentThread = Thread.currentThread();
        if (!threadTransactionMap.containsKey(currentThread)) {
            return;
        }
        Transaction transaction = threadTransactionMap.get(currentThread);
        List<ResourceOperation> finishedOperations = transaction.getFinishedOperations();
        List<ResourceId> operatedResources = transaction.getOperatedResources();
        for (int i = finishedOperations.size() - 1; i >= 0; i--) {
            resources.get(operatedResources.get(i)).unapply(finishedOperations.get(i));
        }
        resourceAllocationGraph.removeNode(transaction);
        threadTransactionMap.remove(currentThread);
    }

    @Override
    public boolean isTransactionActive() {
        return threadTransactionMap.containsKey(Thread.currentThread())
                && threadTransactionMap.get(Thread.currentThread()).isActive();
    }

    @Override
    public boolean isTransactionAborted() {
        return threadTransactionMap.containsKey(Thread.currentThread())
                && threadTransactionMap.get(Thread.currentThread()).isAborted();
    }
}