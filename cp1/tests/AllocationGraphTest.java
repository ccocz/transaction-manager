package cp1.tests;

import cp1.base.ActiveTransactionAborted;
import cp1.base.AnotherTransactionActiveException;
import cp1.base.NoActiveTransactionException;
import cp1.base.Resource;
import cp1.base.ResourceOperationException;
import cp1.base.TransactionManager;
import cp1.base.UnknownResourceIdException;
import cp1.solution.TransactionManagerFactory;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AllocationGraphTest {

    private final static long BASE_WAIT_TIME = 500;

    ResourceImpl r1 = new ResourceImpl(ResourceIdImpl.generate());
    ResourceImpl r2 = new ResourceImpl(ResourceIdImpl.generate());
    ResourceImpl r3 = new ResourceImpl(ResourceIdImpl.generate());

    List<Resource> resources =
            Collections.unmodifiableList(
                    Arrays.asList(r1, r2, r3)
            );
    TransactionManager tm =
            TransactionManagerFactory.newTM(
                    resources,
                    new LocalTimeProviderImpl()
            );

    @Test
    public void testIfDetectsDeadlockWithThreeProcesses() {
        ArrayList<Thread> threads = new ArrayList<Thread>();
        threads.add(
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        System.err.println(Thread.currentThread().getName() + " started");
                        try {
                            Thread.sleep(1 * BASE_WAIT_TIME);
                        } catch (InterruptedException e) {
                        }
                        try {
                            tm.startTransaction();
                        } catch (AnotherTransactionActiveException e) {
                            throw new AssertionError(e);
                        }
                        if (! tm.isTransactionActive()) {
                            throw new AssertionError("Failed to start a transaction");
                        }
                        try {
                            tm.operateOnResourceInCurrentTransaction(
                                    r1.getId(),
                                    ResourceOpImpl.get()
                            );
                            Thread.sleep(4 * BASE_WAIT_TIME);
                            tm.operateOnResourceInCurrentTransaction(
                                    r3.getId(),
                                    ResourceOpImpl.get()
                            );
                            tm.commitCurrentTransaction();
                            if (tm.isTransactionActive()) {
                                throw new AssertionError("Failed to commit a transaction");
                            }
                        } catch (InterruptedException |
                                ActiveTransactionAborted |
                                NoActiveTransactionException |
                                UnknownResourceIdException |
                                ResourceOperationException e) {
                            throw new AssertionError(e);
                        } finally {
                            if (!tm.isTransactionAborted()) {
                                throw new AssertionError("Not aborted after deadlock");
                            }
                            tm.rollbackCurrentTransaction();
                        }
                    }
                })
        );
        threads.add(
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        System.err.println(Thread.currentThread().getName() + " started");
                        try {
                            Thread.sleep(2 * BASE_WAIT_TIME);
                        } catch (InterruptedException e) {
                            throw new AssertionError(e);
                        }
                        try {
                            tm.startTransaction();
                        } catch (AnotherTransactionActiveException e) {
                            throw new AssertionError(e);
                        }
                        if (! tm.isTransactionActive()) {
                            throw new AssertionError("Failed to start a transaction");
                        }
                        try {
                            tm.operateOnResourceInCurrentTransaction(
                                    r2.getId(),
                                    ResourceOpImpl.get()
                            );
                            tm.operateOnResourceInCurrentTransaction(
                                    r1.getId(),
                                    ResourceOpImpl.get()
                            );
                            tm.commitCurrentTransaction();
                        } catch (InterruptedException |
                                ActiveTransactionAborted |
                                NoActiveTransactionException |
                                ResourceOperationException |
                                UnknownResourceIdException e) {
                            throw new AssertionError(e);
                        } finally {
                            tm.rollbackCurrentTransaction();
                            if (tm.isTransactionActive()) {
                                throw new AssertionError("Failed to rollback a transaction");
                            }
                        }
                    }
                })
        );
        threads.add(
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        System.err.println(Thread.currentThread().getName() + " started");
                        try {
                            Thread.sleep(3 * BASE_WAIT_TIME);
                        } catch (InterruptedException e) {
                            throw new AssertionError(e);
                        }
                        try {
                            tm.startTransaction();
                        } catch (AnotherTransactionActiveException e) {
                            throw new AssertionError(e);
                        }
                        if (! tm.isTransactionActive()) {
                            throw new AssertionError("Failed to start a transaction");
                        }
                        if (tm.isTransactionAborted()) {
                            throw new AssertionError("Invalid transaction state");
                        }
                        try {
                            tm.operateOnResourceInCurrentTransaction(
                                    r3.getId(),
                                    ResourceOpImpl.get()
                            );
                            tm.operateOnResourceInCurrentTransaction(
                                    r2.getId(),
                                    ResourceOpImpl.get()
                            );
                            tm.commitCurrentTransaction();
                            if (tm.isTransactionActive()) {
                                throw new AssertionError("Failed to commit a transaction");
                            }
                        } catch (InterruptedException |
                                ActiveTransactionAborted |
                                NoActiveTransactionException |
                                ResourceOperationException |
                                UnknownResourceIdException e) {
                            throw new AssertionError(e);
                        } finally {
                            tm.rollbackCurrentTransaction();
                        }
                    }
                })
        );
        for (Thread t : threads) {
            t.start();
        }
        try {
            for (Thread t : threads) {
                t.join(10 * BASE_WAIT_TIME);
            }
        } catch (InterruptedException e) {
            throw new AssertionError("The main thread has been interrupted");
        }
        expectResourceValue(r1, 1);
        expectResourceValue(r2, 2);
        expectResourceValue(r3, 1);
    }

    private final static void expectResourceValue(ResourceImpl r, long val) {
        if (r.getValue() != val) {
            throw new AssertionError(
                    "For resource " + r.getId() +
                            ", expected value " + val +
                            ", but got value " + r.getValue()
            );
        }
    }
}
