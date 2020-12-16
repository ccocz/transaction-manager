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
import java.util.Collections;
import java.util.List;

public class TransactionManagerImplTest {

    private final static long BASE_WAIT_TIME = 500;

    ResourceImpl r1 = new ResourceImpl(ResourceIdImpl.generate());
    ResourceImpl r2 = new ResourceImpl(ResourceIdImpl.generate());

    List<Resource> resources = Collections.singletonList(r1);
    TransactionManager tm =
            TransactionManagerFactory.newTM(
                    resources,
                    new LocalTimeProviderImpl()
            );

    @Test
    public void testIfGetsAccessAfterRelease() {
        ArrayList<Thread> threads = new ArrayList<Thread>();
        threads.add(
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        System.err.println(Thread.currentThread().getName() + " started");
                        try {
                            Thread.sleep(0 * BASE_WAIT_TIME);
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
                            Thread.sleep(5 * BASE_WAIT_TIME);
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
        expectResourceValue(r1, 2);
    }

    @Test
    public void testNoActiveTransactionException() throws InterruptedException {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    tm.commitCurrentTransaction();
                } catch (NoActiveTransactionException | ActiveTransactionAborted e) {
                    throw new AssertionError(e);
                }
            }
        });
        thread.start();
        thread.join();
    }


    @Test
    public void testAnotherTransactionActiveException() throws InterruptedException {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    tm.startTransaction();
                } catch (AnotherTransactionActiveException e) {
                    throw new AssertionError(e);
                }
                if (! tm.isTransactionActive()) {
                    throw new AssertionError("Failed to start a transaction");
                }
                try {
                    tm.startTransaction();
                } catch (AnotherTransactionActiveException e) {
                    throw new AssertionError(e);
                }
            }
        });
        thread.start();
        thread.join();
    }

    @Test
    public void testUnknownResourceException() throws InterruptedException {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    tm.startTransaction();
                } catch (AnotherTransactionActiveException e) {
                    throw new AssertionError(e);
                }
                try {
                    tm.operateOnResourceInCurrentTransaction(
                            r2.getId(),
                            ResourceOpImpl.get()
                    );
                } catch (InterruptedException |
                        ActiveTransactionAborted |
                        NoActiveTransactionException |
                        UnknownResourceIdException |
                        ResourceOperationException e) {
                    throw new AssertionError(e);
                } finally {
                    tm.rollbackCurrentTransaction();
                }
            }
        });
        thread.start();
        thread.join();
    }

    @Test
    public void testRollbackOperation() throws InterruptedException {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    tm.startTransaction();
                } catch (AnotherTransactionActiveException e) {
                    throw new AssertionError(e);
                }
                try {
                    for (int i = 1; i <= 100; i++) {
                        tm.operateOnResourceInCurrentTransaction(
                                r1.getId(),
                                ResourceOpImpl.get()
                        );
                    }
                    tm.rollbackCurrentTransaction();
                } catch (InterruptedException |
                        ActiveTransactionAborted |
                        NoActiveTransactionException |
                        UnknownResourceIdException |
                        ResourceOperationException e) {
                    throw new AssertionError(e);
                } finally {
                    tm.rollbackCurrentTransaction();
                }
            }
        });
        thread.start();
        thread.join();
        expectResourceValue(r1, 0);
    }

    @Test
    public void testCommitOperation() throws InterruptedException {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    tm.startTransaction();
                } catch (AnotherTransactionActiveException e) {
                    throw new AssertionError(e);
                }
                try {
                    for (int i = 1; i <= 100; i++) {
                        tm.operateOnResourceInCurrentTransaction(
                                r1.getId(),
                                ResourceOpImpl.get()
                        );
                    }
                    tm.commitCurrentTransaction();
                } catch (InterruptedException |
                        ActiveTransactionAborted |
                        NoActiveTransactionException |
                        UnknownResourceIdException |
                        ResourceOperationException e) {
                    throw new AssertionError(e);
                } finally {
                    tm.rollbackCurrentTransaction();
                }
            }
        });
        thread.start();
        thread.join();
        expectResourceValue(r1, 100);
    }

    @Test
    public void testIsTransactionActive() {
        assert !tm.isTransactionActive();
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
