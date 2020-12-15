package cp1.tests;

import cp1.solution.Transaction;
import org.junit.Test;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TransactionTest {

    @Test
    public void testEqualsAndHashcode() {
        Transaction transaction = new Transaction(Thread.currentThread(), System.currentTimeMillis());
        ConcurrentMap<Transaction, Boolean> map = new ConcurrentHashMap<>();
        map.put(transaction, true);
        assert map.containsKey(transaction);
        Transaction transaction1 = new Transaction(Thread.currentThread(), System.currentTimeMillis());
        assert map.containsKey(transaction1);
    }

}
