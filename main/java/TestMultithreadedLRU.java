import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Created by aleks on 7/29/15.
 * This class will simulate concurrent read and write accesses to the cache by multiple clients
 */
public class TestMultithreadedLRU {

    static Logger log = Logger.getLogger("TestMultithreadedLRU.class");

    @Test
    /*
    Verify that multiple clients can all write their data to a single cache without stepping over one another
     */
    public void testMultipleClientsWithSingleCache() throws InterruptedException, ExecutionException {

        final int MULTIPLE_CACHE_COUNT = 1;
        runMultipleClientsMultipleCachesTest(MULTIPLE_CACHE_COUNT);
    }

    @Test
    /*
    Verify that multiple groups of clients can all write their data to a given cache without stepping over one another.
    The purpose of this test is to shake out any bugs with static methods and blocking calls that would normally
    not get hit if there is a single queue instance running in the JRE.

    This will be done by having each client interact with multiple caches at a  time
     */
    public void testMultipleClientsMultipleCaches() throws InterruptedException, ExecutionException {

        final int MULTIPLE_CACHE_COUNT = 25;
        runMultipleClientsMultipleCachesTest(MULTIPLE_CACHE_COUNT);
    }

    private class SampleClient<K, V> implements Runnable {

        final int clientId;
        final LRUCache<K, V> cacheToTest;
        HashSet<LRUCache.Entry<K, V>> keyValuesToInsert;

        public SampleClient(final int clientId, final LRUCache<K, V> cacheToTest, final HashSet<LRUCache.Entry<K, V>> keyValuesToInsert) {

            this.clientId = clientId;
            this.cacheToTest = cacheToTest;
            this.keyValuesToInsert = keyValuesToInsert;
        }

        @Override
        public void run() {

            int sleepTime = (int) (Math.random() * 1 * 1000);
            log.fine("Thread " + clientId + " sleeping for " + sleepTime + " milliseconds");

            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            for (LRUCache.Entry<K, V> entry : keyValuesToInsert) {

                try {
                    Thread.sleep(6);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                cacheToTest.writeValueToCache(entry.getKey(), entry.getValue());
            }

            log.fine("From client " + this.clientId + ": hi");
            log.info(cacheToTest.toString());
        }
    }

    private HashSet<LRUCache.Entry<String, Double>> generateDataSet(final int clientId, final int numEntries) {

        HashSet<LRUCache.Entry<String, Double>> entrySet = new HashSet<LRUCache.Entry<String, Double>>();
        for (int i = 0; i < numEntries; i++) {

            //Adding both values to overwrite, and new values
            entrySet.add(new LRUCache.Entry<String, Double>("client_" + clientId, Double.valueOf(i)));
            entrySet.add(new LRUCache.Entry<String, Double>("client_" + clientId + "_" + i, Double.valueOf(i)));
        }

        log.info("There should be " + entrySet.size() + " entries for thread " + clientId);

        return entrySet;
    }

    //Start at the head. Go until you detect a loop
    public static void detectLoop(LRUCache cache, final int expectedSize) {

        synchronized (cache) {
            LRUCache.Node head = cache.getOrderedList().getHead();
            HashSet<LRUCache.Node> itemsSeen = new HashSet<LRUCache.Node>();

            LRUCache.Node current = head;
            StringBuilder sb = new StringBuilder("listOfNodes: ");

            int itemsSeenCount = 0;
            while (current != null) {

                itemsSeenCount++;

                sb.append(current + " --> ");
                if (itemsSeen.contains(current)) {
                    Assert.fail("Loop detected!!! Output: " + sb.toString());
                    throw new RuntimeException("Loop detected");
                }
                current = current.getNext();

                if (itemsSeenCount > (expectedSize)) {
                    log.severe("Seen too many items. Expected " + expectedSize + " but saw " + itemsSeenCount);
                    break;
                }
            }
        }
    }

    private void runMultipleClientsMultipleCachesTest(final int NUM_CACHES_TO_TEST) throws InterruptedException, ExecutionException {
        final int NUM_CONCURRENT_CACHES = NUM_CACHES_TO_TEST;
        final int CONCURRENT_READ_CLIENTS = 20;
        final int CACHE_SIZE = 12;
        final int ITEMS_PER_CLIENT = CACHE_SIZE + 50; //guaranteeing that we have more than

        final List<LRUCache> LRUCacheList = new ArrayList<LRUCache>();
        for (int i = 0; i < NUM_CONCURRENT_CACHES; i++) {

            LRUCacheList.add(new LRUCache<String, Double>(CACHE_SIZE));
        }

        ExecutorService execService = Executors.newFixedThreadPool(CONCURRENT_READ_CLIENTS);
        List<Future> futureList = new ArrayList<Future>();
        for (int i = 0; i < CONCURRENT_READ_CLIENTS; i++) {

            HashSet<LRUCache.Entry<String, Double>> dataToInsert = generateDataSet(i, ITEMS_PER_CLIENT);
            for (LRUCache cacheToTest : LRUCacheList) {

                futureList.add(execService.submit(new SampleClient<>(i, cacheToTest, dataToInsert)));
            }
        }

        execService.shutdown();
        execService.awaitTermination(1, TimeUnit.SECONDS);

        for (Future f : futureList) {
            if (f.get() != null || !f.isDone() || f.isCancelled()) {
                log.severe("Problem");
                throw new RuntimeException("Problem");
            }
        }

        //Verify the count to be correct
        for (LRUCache cacheToTest : LRUCacheList) {
            Assert.assertEquals("Incorrect cache size detected", CACHE_SIZE, cacheToTest.getCacheSize());

            log.fine("About to test the cache for loops");
            detectLoop(cacheToTest, cacheToTest.getCacheSize());

            log.fine("About to print the output: ");
            log.fine(cacheToTest.toString());
        }
    }
}
