package cp1.solution;

import cp1.base.Resource;
import cp1.base.ResourceId;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

/**
 * Using resource allocation graph, we can detect deadlocks and fix it aborting latest
 * transaction. Also, `AllocationGraph` provides corresponding method to finish a transaction
 * freeing all the assets it had a disposition with.
 *
 * Forementioned methods are thread safe.
 *
 * @author Resul Hangeldiyev
 */
public class AllocationGraph {

    enum Node {
        NOT_VISITED,
        DONE,
        IN_STACK
    }

    private final ConcurrentMap<Transaction, ResourceId> resourceAllocationGraph;
    private final ConcurrentMap<ResourceId, Queue<Transaction>> resourceWaitingQueue;
    private final ConcurrentMap<ResourceId, Transaction> resourceOwners;

    public AllocationGraph(Collection<Resource> resources) {
        this.resourceAllocationGraph = new ConcurrentHashMap<>();
        this.resourceWaitingQueue = new ConcurrentHashMap<>();
        this.resourceOwners = new ConcurrentHashMap<>();
        for (Resource resource : resources) {
            resourceWaitingQueue.putIfAbsent(resource.getId(), new ConcurrentLinkedQueue<>());
        }
    }

    public synchronized boolean addEdgeIfNecessary(Transaction from, ResourceId rid) {
        if (resourceOwners.containsKey(rid)) {
            resourceAllocationGraph.putIfAbsent(from, rid);
            resourceWaitingQueue.get(rid).add(from);
            detectCycle(from);
            return true;
        } else {
            resourceOwners.put(rid, from);
            from.newAcquiredResource(rid);
            return false;
        }
    }

    public synchronized void removeNode(Transaction node) {
        if (resourceAllocationGraph.containsKey(node)) {
            resourceWaitingQueue.get(resourceAllocationGraph.get(node)).remove(node);
            resourceAllocationGraph.remove(node);
        }
        for (ResourceId rid: node.getAcquiredResources()) {
            if (resourceWaitingQueue.get(rid).isEmpty()) {
                resourceOwners.remove(rid);
                continue;
            }
            Transaction next = resourceWaitingQueue.get(rid).remove();
            resourceOwners.replace(rid, next);
            resourceAllocationGraph.remove(next);
            next.newAcquiredResource(rid);
            next.getSemaphore().release();
        }
    }

    private void detectCycle(Transaction start) {
        Stack<Transaction> stack = new Stack<>();
        Map<Transaction, Node> visited = new HashMap<>();
        for (Transaction transaction : resourceAllocationGraph.keySet()) {
            visited.put(transaction, Node.NOT_VISITED);
        }
        dfs(start, stack, visited);
    }

    private void dfs(Transaction start, Stack<Transaction> stack, Map<Transaction, Node> visited) {
        stack.push(start);
        visited.put(start, Node.IN_STACK);
        ResourceId ridAdj = resourceAllocationGraph.get(start);
        Transaction adj;
        if (ridAdj == null || ((adj = resourceOwners.get(ridAdj)).isAborted())) {
            visited.put(stack.pop(), Node.DONE);
            return;
        }
        if (visited.get(adj) == Node.IN_STACK) {
            handleCycle(stack, adj);
        } else if (visited.get(adj) == Node.NOT_VISITED) {
            dfs(adj, stack, visited);
        }
        visited.put(stack.pop(), Node.DONE);
    }

    private void handleCycle(Stack<Transaction> stack, Transaction start) {
        Stack<Transaction> cycle = new Stack<>();
        cycle.push(stack.pop());
        while (!cycle.peek().equals(start)) {
            cycle.push(stack.pop());
        }
        Transaction toBeCancelled = cycle.pop();
        stack.push(toBeCancelled);
        while (!cycle.empty()) {
            Transaction transaction = cycle.pop();
            if (transaction.getStartingTime() > toBeCancelled.getStartingTime()) {
                toBeCancelled = transaction;
            } else if (transaction.getStartingTime() == toBeCancelled.getStartingTime()
                    && transaction.getThread().getId() > toBeCancelled.getThread().getId()) {
                toBeCancelled = transaction;
            }
            stack.push(transaction);
        }
        toBeCancelled.abort();
        toBeCancelled.getSemaphore().release();
    }

}
