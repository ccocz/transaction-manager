package cp1.solution;

import cp1.base.Resource;
import cp1.base.ResourceId;

import java.util.Collection;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

public class AllocationGraph {

    enum Node {
        NOT_VISITED,
        DONE,
        IN_STACK
    }

    private final ConcurrentMap<Transaction, Transaction> resourceAllocationGraph;
    private final ConcurrentMap<ResourceId, Queue<Transaction>> resourceWaitingQueue;
    private final ConcurrentMap<ResourceId, Transaction> resourceOwners;

    public AllocationGraph(Collection<Resource> resources, ConcurrentMap<ResourceId, Transaction> resourceOwners) {
        this.resourceAllocationGraph = new ConcurrentHashMap<>();
        this.resourceWaitingQueue = new ConcurrentHashMap<>();
        this.resourceOwners = resourceOwners;
        for (Resource resource : resources) {
            resourceWaitingQueue.putIfAbsent(resource.getId(), new ConcurrentLinkedQueue<>());
        }
    }

    public void addEdge(Transaction from, Transaction to, ResourceId rid) {
        resourceAllocationGraph.putIfAbsent(from, to);
        resourceWaitingQueue.get(rid).add(from);
    }

    public void removeNode(Transaction node) {
        for (ResourceId rid: node.getAcquiredResources()) {
            if (resourceWaitingQueue.get(rid).isEmpty()) {
                resourceOwners.remove(rid);
                continue;
            }
            Transaction next = resourceWaitingQueue.get(rid).remove();
            for (Transaction remaining : resourceWaitingQueue.get(rid)) {
                resourceAllocationGraph.put(remaining, next);
            }
            resourceOwners.replace(rid, next);
            next.getSemaphore().release();
        }
        resourceAllocationGraph.remove(node);
    }

    public void detectCycle(Transaction start) {
        Stack<Transaction> stack = new Stack<>(); // todo: multi-thread version
        ConcurrentMap<Transaction, Node> visited = new ConcurrentHashMap<>();
        for (Transaction transaction : resourceAllocationGraph.keySet()) {
            visited.put(transaction, Node.NOT_VISITED);
        }
        dfs(start, stack, visited);
    }

    private void dfs(Transaction start, Stack<Transaction> stack, ConcurrentMap<Transaction, Node> visited) {
        stack.push(start);
        visited.put(start, Node.IN_STACK);
        Transaction adj = resourceAllocationGraph.get(start);
        if (adj == null || adj.isAborted()) {
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
        while (cycle.peek() != start) {
            cycle.push(stack.pop());
        }
        Transaction toBeCancelled = cycle.pop();
        stack.push(toBeCancelled);
        while (!cycle.empty()) {
            Transaction transaction = cycle.pop();
            if (transaction.getStartingTime() < toBeCancelled.getStartingTime()) {
                toBeCancelled = transaction;
            } else if (transaction.getStartingTime() == toBeCancelled.getStartingTime()
                    && transaction.getThread().getId() > toBeCancelled.getThread().getId()) {
                toBeCancelled = transaction;
            }
            stack.push(transaction);
        }
        toBeCancelled.abort();
        toBeCancelled.getThread().interrupt();
    }

}
