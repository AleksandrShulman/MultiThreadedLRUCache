import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Created by aleks on 7/25/15.
 */

/*
Goals:


1. Maintain a sorted order of items in the queue
2. Check if something is in the cache in O(1) time
3. Put something in the cache in O(1) time

Implementation:

1. There will be two data structures, in order to optimize both reads and writes
2.

Obstacles:
1. In a traditional linked list implementation, the lookup time is O(n), because you need to search every element to check that it's there.
2. That's why we have a hashset, where lookups are O(1).
3. But Hashsets aren't ordered.
4. So a list has to be ordered. So we'll use the list for ordering. But, the traditional get() function is O(n).
5. So we need to create a new list. This list will
6. Hashmap will have a reference to the object in the list
 */

public class LRUCache<K, V> {

    public static final int MAX_CACHE_SIZE = 1500;

    Logger log = Logger.getLogger("LRUCache.class");
    int configuredQueueMaxSize;

    // This is a mapping from the key-value entry, to the node in the doubly-linked-list
    final HashMap<K, Node> entrySet;
    final LRUOptimizedLinkedList orderedList;

    final static String INVALID_RESIZE_ERROR_MSG = "Size needs to be between 1 and " + MAX_CACHE_SIZE;

    public LRUCache(int configuredQueueMaxSize) {

        if (configuredQueueMaxSize < 1 || configuredQueueMaxSize > MAX_CACHE_SIZE) {
            final String ERROR_MSG = "Size needs to be between 1 and \" + MAX_CACHE_SIZE";
            log.severe(ERROR_MSG);
            throw new IllegalStateException(ERROR_MSG);
        }
        this.configuredQueueMaxSize = configuredQueueMaxSize;
        entrySet = new HashMap<K, Node>();
        orderedList = new LRUOptimizedLinkedList(configuredQueueMaxSize);
    }

    public synchronized void resizeContainer(final int newConfiguredSize) {

        if (newConfiguredSize < 1 || newConfiguredSize > MAX_CACHE_SIZE) {
            log.severe(INVALID_RESIZE_ERROR_MSG);
            throw new IllegalStateException(INVALID_RESIZE_ERROR_MSG);

        }

        //Logic: If the size is greater than the current size, then there isn't much to do
        //       If the size is less than the current size, we need to expel items from the queue
        //TODO: Implement this

        synchronized (this) {

            if (this.configuredQueueMaxSize < newConfiguredSize) {

                this.configuredQueueMaxSize = newConfiguredSize;
                return;
            }

            // find how many nodes we need to delete and go from the tail forwards to delete them
            final int NUM_NODES_TO_DELETE = configuredQueueMaxSize - newConfiguredSize;
            this.configuredQueueMaxSize = newConfiguredSize;

            log.info("Need to remove " + NUM_NODES_TO_DELETE + " nodes");
            Set<Node> nodesToRemove = orderedList.trimList(NUM_NODES_TO_DELETE);

            log.info("We have " + nodesToRemove.size() + " nodes we need to remove from entrySet");
            for (Node n : nodesToRemove) {
                entrySet.remove(n.getEntry().getKey());
            }

            if (entrySet.size() != orderedList.getCurrentSize()) {
                final String ERROR_MSG = "Set and List diverged in terms of size. Set size is " + entrySet.size() +
                        " and list contains " + orderedList.currentSize + " items";
                log.severe(ERROR_MSG);
                throw new IllegalStateException(ERROR_MSG);
            }
        }
    }

    public synchronized V getValueFromCache(final K key) {

        if (orderedList == null || entrySet == null) {
            throw new RuntimeException("Cache not initialized yet.");
        }

        return (V) entrySet.get(key).getEntry().getValue();
    }

    public boolean existsInCache(K key) {

        return entrySet.containsKey(key);
    }

    public boolean existsInCache(K key, V value) {

        // Let's assume null is not a valid value
        if (value == null) {

            throw new RuntimeException("Value cannot be null");
        }

        Node keyEntry = entrySet.get(key);
        if (keyEntry == null) {
            return false;
        }

        if (keyEntry.getEntry().getValue().equals(value)) {
            return true;
        }

        return false;
    }

    public int getPriorityInCacheOfObject(K key) {

        return orderedList.getIndexOfObject(key);
    }

    public LRUOptimizedLinkedList getOrderedList() {
        return this.orderedList;
    }

    public int getCacheSize() {

        final int entrySetSize = entrySet.size();
        final int llSize = orderedList.getCurrentSize();

        if (llSize != entrySetSize) {
            throw new RuntimeException("Mismatch in data structure sizes - " + entrySetSize + " vs. " + llSize);
        }

        return entrySet.size();
    }

    /*
    The purpose and scope of this function is to coordinate the two data structures. It will tell both of them to either
    add or remove an element.

    The important thing here is to avoid leaking implementation details. Only problem is that it may have more visibility to do things
    in the right time-complexity that the function itself does not have.
     */
    public synchronized void writeValueToCache(K key, V value) {

        Entry entryToWrite = new Entry(key, value);

        Node existingListNode = entrySet.get(key);
        if (existingListNode == null) {

            // creating a new node
            Node tailNodeToBeDeletedUponSuccessfulInsert = orderedList.getTail();
            Node newNode = null;
            if (this.getCacheSize() == this.configuredQueueMaxSize) {

                entrySet.remove(tailNodeToBeDeletedUponSuccessfulInsert.getEntry().getKey());
            }

            // the insert will remove the node if the size limit is exceeded
            newNode = orderedList.insert(entryToWrite);
            if (newNode == null) {
                throw new RuntimeException("The outcome of insert produced a null node. This is not supposed to happen. Exiting.");
            }

            entrySet.put(key, newNode);
        } else {

            // updating an existing entry
            V existingValue = (V) existingListNode.getEntry().getValue();
            if (!existingValue.equals(value)) {

                // it's already there, just need to add it to the front
                existingListNode.getEntry().setValue(value);
            }

            orderedList.setHead(existingListNode);
        }
    }

    @Override
    public synchronized String toString() {

        StringBuilder sb = new StringBuilder();
        final int EXPECTED_NODES_TO_SEE = this.getCacheSize();
        if (orderedList != null) {
            Node currentNode = orderedList.getHead();
            int nodesSeen = 0; // in case currentNode is not initialized, 0 is the correct size
            while (currentNode != null) {

                nodesSeen++; //for each time a node is not null, we add one, starting at 0.
                if (currentNode.getNext() == currentNode) {
                    throw new RuntimeException("CurrentNode's next entry " + currentNode.getEntry() + " points to itself");
                }

                if (currentNode.getPrevious() == currentNode) {
                    throw new RuntimeException("CurrentNode's next entry " + currentNode.getEntry() + " points to itself");
                }

                sb.append(currentNode.getEntry().getKey() + " : " + currentNode.getEntry().getValue());
                if (currentNode.next != null) {

                    sb.append(" -> ");
                }

                currentNode = currentNode.getNext();

                if (nodesSeen > (EXPECTED_NODES_TO_SEE)) {

                    final String ERROR_MSG = "Too many nodes." +
                            " Expected " + EXPECTED_NODES_TO_SEE + " but have seen already " +
                            nodesSeen + ". Probably a loop or bad eviction mechanism...";

                    log.severe(ERROR_MSG);
                    throw new RuntimeException(ERROR_MSG);
                }
            }

            if (nodesSeen < EXPECTED_NODES_TO_SEE) {
                final String ERROR_MSG = "Too few nodes. " +
                        " Expected " + EXPECTED_NODES_TO_SEE + " but have seen only " + nodesSeen;
                log.severe(ERROR_MSG);
                throw new RuntimeException(ERROR_MSG);
            }
        }

        return sb.toString();
    }

    protected class Node {

        private Entry entry;
        private Node next;
        private Node previous;

        public Node(Entry entry, Node next, Node previous) {
            this.entry = entry;
            this.next = next;
            this.previous = previous;
        }

        public Entry getEntry() {
            return this.entry;
        }

        public Node getNext() {
            return this.next;
        }

        public Node getPrevious() {
            return this.previous;
        }

        public void setNext(Node next) {

            if (this == next) {
                throw new IllegalStateException("Attempted to produce a loop at " + this.toString());
            }

            this.next = next;
        }

        public void setPrevious(Node previous) {

            if (this == previous) {
                throw new IllegalStateException("Attempted to produce a loop at " + this.toString());
            }

            this.previous = previous;
        }

        public void setEntry(Entry entry) {
            this.entry = entry;
        }

        @Override
        public String toString() {

            StringBuilder sb = new StringBuilder();

            final String previousNodeString;
            if (this.getPrevious() != null) {

                previousNodeString = this.getPrevious().getEntry().toString();
            } else {
                previousNodeString = "X";
            }

            final String nextNodeString;
            if (this.getNext() != null) {

                nextNodeString = this.getNext().getEntry().toString();
            } else {
                nextNodeString = "X";
            }

            sb.append(previousNodeString);
            sb.append(" <-- ");
            sb.append(this.entry.toString());
            sb.append(" --> ");
            sb.append(nextNodeString);
            return sb.toString();
        }
    }

    public static class Entry<K, V> {

        private K key;
        private V value;

        public Entry(K key, V value) {

            this.key = key;
            this.value = value;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        public void setValue(V value) {

            this.value = value;
        }

        @Override
        public String toString() {

            StringBuilder sb = new StringBuilder();
            sb.append(this.getKey());
            sb.append(" : ");
            sb.append(this.getValue());

            return sb.toString();
        }

        @Override
        public boolean equals(Object o) {

            if (!o.getClass().equals(Entry.class)) {
                return false;
            }

            if (!this.key.equals(((Entry) o).getKey())) {

                return false;
            }

            if (!this.value.equals(((Entry) o).getValue())) {
                return false;
            }

            return true;
        }
    }

    // This is an implementation of a doubly-linked-list
    public class LRUOptimizedLinkedList {

        private Node head;
        private Node tail;
        int currentSize;
        int maxSize;

        public LRUOptimizedLinkedList(int maxSize) {
            this.currentSize = 0;
            this.maxSize = maxSize;
            this.head = null;
            this.tail = null;
        }

        public Node getHead() {
            return head;
        }

        public Node getTail() {
            return tail;
        }

        public int getCurrentSize() {
            return currentSize;
        }

        public synchronized void setHead(Node newHead) {

            if (this.getHead() == newHead) {

                log.fine("the node we are promoting to head is already head.");
                return;
            }

            // this will perform all the machinations to make the given node the head.
            if (this.getHead() == null) {

                this.head = newHead;
                return;

            } else {

                // If we're moving an existing node to the head, we also need to fix the
                // place in the list where the gap will be.

                if (newHead == tail) {

                    log.fine("Promoting tail to head");
                    newHead.getPrevious().setNext(null);
                    tail = tail.getPrevious();

                } else if (newHead.getNext() != null && newHead.getPrevious() != null) {

                    newHead.getNext().setPrevious(newHead.getPrevious());
                    newHead.getPrevious().setNext(newHead.getNext());
                } else if (newHead.getNext() == head) {

                    newHead.setPrevious(null);
                    //if we're adding a new entry, it's not anything yet
                } else {
                    throw new IllegalArgumentException("Having a hard time placing where this node is and what to do with it");
                }

                newHead.setNext(this.head);
                this.head.setPrevious(newHead);
                newHead.setPrevious(null);
                this.head = newHead;
            }
        }

        public synchronized void setTail(Node tail) {
            this.tail = tail;
        }

        // By definition only called for a new node
        public synchronized Node insert(Entry e) {

            // On insert, there are two options. First option is that the item is already there. We know it's there, because
            Node listEntry = entrySet.get(e);
            if (listEntry == null) {
                // it's not there. Let's add it then.
                // get the head node, and make it the node after this node is to be inserted

                if (head == null) {

                    // this is the first entry into the list
                    head = new Node(e, null, null);
                    tail = head; //if there is no head entry, head and tail are one
                    currentSize++;
                    return head;
                } else {

                    // this is a new entry into a list with at least one item
                    Node newNode = new Node(e, head, null);
                    setHead(newNode);

                    // do some sanity checks
                    if (newNode.getNext() == null) {
                        throw new RuntimeException("When we're adding a node to the head of an existing list, the node after head should not be null");
                    }

                    if (getHead().getNext() == null) {
                        throw new RuntimeException("Head's next node is null. Should not be.");
                    }

                    if (getHead().getNext().getPrevious() != head) {

                        throw new IllegalStateException("Head->Next->Previous isn't head!?");
                    }
                    currentSize++;

                    //after this insertion, check if we've exceeded size
                    if (this.currentSize > this.maxSize) {

                        //move to tail, move it back up one, and then cut the last node loose

                        if (tail.getNext() != null) {
                            throw new IllegalStateException("The tail's next node seems to not be null.");
                        }

                        Node newTail = tail.getPrevious();
                        if (newTail != null) {
                            newTail.setNext(null);
                        }
                        tail.setPrevious(null);
                        tail = newTail;
                        currentSize--;
                    }

                    return newNode;
                }
            } else {

                throw new IllegalStateException("This code path is for new nodes only!!!");
                // if we're doing an insert, then just make sure it's at the head.
                //setHead(listEntry);
                //return head;
            }
        }

        public synchronized Set<Node> trimList(final int TRIM_COUNT) {

            Node currentTail = tail;
            Set<Node> nodesToDelete = new HashSet<Node>();

            for (int nodesTrimmed = 0; nodesTrimmed < TRIM_COUNT; nodesTrimmed++) {

                if (currentTail == null) {
                    throw new IllegalStateException("We have more nodes to delete, yet list is empty!?");
                }

                if (currentTail.getPrevious() == null && currentTail != head) {
                    throw new IllegalStateException("Attempting to delete node but cannot back up to previous. We're also not at head");
                }

                Node nodeToDelete = currentTail;
                nodesToDelete.add(nodeToDelete);
                currentTail = currentTail.getPrevious();
                nodeToDelete.setNext(null);
                nodeToDelete.setPrevious(null);
                currentSize--;
                tail = currentTail;
                tail.setNext(null);
            }

            return nodesToDelete;
        }

        // O(n) method to get how far into the queue the object is. Used as a test hook.
        public int getIndexOfObject(final K keyToLookup) {

            int returnIndex = 0;

            Node current = head;
            while (current != null) {

                if (current.getEntry().getKey().equals(keyToLookup))
                    return returnIndex;

                current = current.getNext();
                returnIndex++;
            }

            // -1 indicates that the item was not found
            return -1;
        }
    }
}
