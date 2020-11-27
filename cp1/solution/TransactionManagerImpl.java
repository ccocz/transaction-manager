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
import java.util.concurrent.Semaphore;

public class TransactionManagerImpl implements TransactionManager {

    private final ConcurrentMap<Thread, Transaction> threadTransactionMap;
    private final ConcurrentMap<Resource, Semaphore> resources;
    private final LocalTimeProvider timeProvider;

    public TransactionManagerImpl(Collection<Resource> resources, LocalTimeProvider localTimeProvider) {
        this.resources = new ConcurrentHashMap<>();
        for (Resource resource : resources) {
            this.resources.put(resource, new Semaphore(1));
        }
        this.timeProvider = localTimeProvider;
        this.threadTransactionMap = new ConcurrentHashMap<>();
    }

    @Override
    public void startTransaction() throws AnotherTransactionActiveException {
        Thread currentThread = Thread.currentThread();
        if (threadTransactionMap.containsKey(currentThread)) {
            throw new AnotherTransactionActiveException();
        }
        threadTransactionMap.put(currentThread, new Transaction());
    }

    @Override
    public void operateOnResourceInCurrentTransaction(ResourceId rid, ResourceOperation operation) throws NoActiveTransactionException,
            UnknownResourceIdException,
            ActiveTransactionAborted,
            ResourceOperationException,
            InterruptedException {
        Thread currentThread = Thread.currentThread();
        if (!threadTransactionMap.containsKey(currentThread)) {
            throw new NoActiveTransactionException();
        }
        Transaction transaction = threadTransactionMap.get(currentThread);
        Resource resource = getResourceWithIdOrNull(rid);
        if (resource == null) {
            throw new UnknownResourceIdException(rid);
        }
        if (threadTransactionMap.get(currentThread).isAborted()) {
            throw new ActiveTransactionAborted();
        }
        if (!transaction.isAccessAcquiredForResource(resource)) {

            // todo: handle deadlocks
            System.err.println("Thread: " + currentThread.getName() + " waiting for " + rid);
            resources.get(resource).acquire();
            transaction.newAcquiredResource(resource);
        }
        resource.apply(operation);
        transaction.finishedOperationOnTheResource(resource, operation);
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
        for (Resource resource : transaction.getAcquiredResources()) {
            resources.get(resource).release();
        }
        threadTransactionMap.remove(currentThread);
    }

    @Override
    public void rollbackCurrentTransaction() {
        Thread thread = Thread.currentThread();
        if (!threadTransactionMap.containsKey(thread)) {
            return;
        }
        Transaction transaction = threadTransactionMap.get(thread);
        List<ResourceOperation> finishedOperations = transaction.getFinishedOperations();
        List<Resource> operatedResources = transaction.getOperatedResources();
        for (int i = finishedOperations.size() - 1; i >= 0; i--) {
            operatedResources.get(i).unapply(finishedOperations.get(i));
        }
        for (Resource resource : transaction.getAcquiredResources()) {
            resources.get(resource).release();
        }
        threadTransactionMap.remove(Thread.currentThread());
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

    private Resource getResourceWithIdOrNull(ResourceId rid) {
        for (Resource resource : resources.keySet()) {
            if (resource.getId().equals(rid)) {
                return resource;
            }
        }
        return null;
    }
}