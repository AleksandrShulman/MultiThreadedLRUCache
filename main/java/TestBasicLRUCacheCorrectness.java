import org.junit.Test;
import org.junit.Assert;

import java.util.logging.Logger;

/**
 * Created by aleks on 7/25/15.
 */
public class TestBasicLRUCacheCorrectness {


    Logger log = Logger.getLogger("TestBasicLRUCacheCorrectness.class");

    final static int VALID_LRU_CACHE_SIZE = 40;
    final static int LRU_CACHE_SIZE_BAD = LRUCache.MAX_CACHE_SIZE + 1;
    final static String KEY_STRING_1="KEY_1";
    final static String KEY_STRING_2="KEY_2";

    final Double VAL_1 = new Double(45);
    final Double VAL_2 = new Double(46);

    @Test
    public void testEmptyNewCache() {

        LRUCache myCache = new LRUCache(VALID_LRU_CACHE_SIZE);

        try {

            LRUCache badCache = new LRUCache(LRU_CACHE_SIZE_BAD);
            Assert.fail("Created object despite bad cache size");
        } catch(RuntimeException rte) {

            log.fine("Correct behavior: Caught exception because: " + rte.getMessage());
        }
    }

    @Test
    public void testResizeCache() {

    }


    @Test
    public void testClearCache() {


    }

    @Test
    public void testBasicInsert() {

        final Double D = new Double(45);
        final Double NO_SUCH_VALUE = new Double(55);
        final String KEY_STRING = "someDoubleValue";
        final String NO_SUCH_KEY = "someDoubleValue_noSuchString";

        LRUCache<String,Double> doubleCache = new LRUCache<String,Double>(TestBasicLRUCacheCorrectness.VALID_LRU_CACHE_SIZE);
        doubleCache.writeValueToCache(KEY_STRING, D);

        Assert.assertTrue(doubleCache.existsInCache(KEY_STRING));
        Assert.assertTrue(!doubleCache.existsInCache(NO_SUCH_KEY));

        Assert.assertTrue(doubleCache.existsInCache(KEY_STRING,D));
        Assert.assertTrue(!doubleCache.existsInCache(KEY_STRING,NO_SUCH_VALUE));
    }

    @Test
    public void testMultipleInsertions() {

        LRUCache<String,Double> testCache = new LRUCache<String,Double>(TestBasicLRUCacheCorrectness.VALID_LRU_CACHE_SIZE);

        testCache.writeValueToCache(KEY_STRING_1, VAL_1);
        testCache.writeValueToCache(KEY_STRING_2, VAL_2);

        Assert.assertTrue(testCache.existsInCache(KEY_STRING_1));
        Assert.assertTrue(testCache.existsInCache(KEY_STRING_2));

        Assert.assertEquals("The first object is not in its correct place in the queue",1, testCache.getPriorityInCacheOfObject(KEY_STRING_1));
        Assert.assertEquals("The second object is not in its correct place in the queue",0, testCache.getPriorityInCacheOfObject(KEY_STRING_2));

        LRUCache.Node tail = testCache.getOrderedList().getTail();
        LRUCache.Node head = testCache.getOrderedList().getHead();

        //This is true when there are strictly two unique cache values
        Assert.assertTrue("Tail not connected to head", tail.getPrevious()==head);
        Assert.assertTrue("Head not connected to tail", head.getNext()==tail);

        // Now re-write the first value. Orders should change
        testCache.writeValueToCache(KEY_STRING_1, VAL_1);
        Assert.assertEquals("The first object is not in its correct place in the queue",0, testCache.getPriorityInCacheOfObject(KEY_STRING_1));
        Assert.assertEquals("The second object is not in its correct place in the queue",1, testCache.getPriorityInCacheOfObject(KEY_STRING_2));
    }

    @Test
    public void testHeadSetCorrectly() {

        LRUCache<String,Double> testCache = new LRUCache<String,Double>(TestBasicLRUCacheCorrectness.VALID_LRU_CACHE_SIZE);
        testCache.writeValueToCache(KEY_STRING_1, VAL_1);

        Assert.assertTrue(testCache.getOrderedList().getHead().getEntry().getValue().equals(VAL_1));
    }

    @Test
    public void testOverwriteCachedEntry() {

        LRUCache<String,Double> testCache = new LRUCache<String,Double>(TestBasicLRUCacheCorrectness.VALID_LRU_CACHE_SIZE);
        final Double NEW_VAL_1 = new Double(VAL_2+1);

        testCache.writeValueToCache(KEY_STRING_1, VAL_1);
        testCache.writeValueToCache(KEY_STRING_2, VAL_2);
        testCache.writeValueToCache(KEY_STRING_1, NEW_VAL_1);

        //verify it's at the head of the queue
        Assert.assertEquals("The first object is not in its correct place in the queue",0, testCache.getPriorityInCacheOfObject(KEY_STRING_1));

        //verify that the correct value is there
        testCache.getValueFromCache(KEY_STRING_1);
        Assert.assertEquals("The returned value is not correct",NEW_VAL_1, testCache.getValueFromCache(KEY_STRING_1));
        Assert.assertEquals("Incorrect size returned", 2, testCache.getCacheSize());
    }

    @Test
    public void testSizeLimitEnforced() {

        //TODO: With cache size=2, something isn't quite right. We have an array of size 3!!
        for (int CACHE_SIZE = 1; CACHE_SIZE < 57; CACHE_SIZE++) {

            log.fine("TRACE: Working with cache size " + CACHE_SIZE);
            LRUCache<String, Double> testCache = new LRUCache<String, Double>(CACHE_SIZE);
            Assert.assertEquals("Initial LRU cache size incorrect", 0, testCache.getCacheSize());

            for (int i = 0; i < CACHE_SIZE; i++) {

                String key = "key_" + i;
                testCache.writeValueToCache(key, Double.valueOf(i));
                Assert.assertTrue(testCache.existsInCache(key));
            }

            Assert.assertEquals("After " + CACHE_SIZE + " inserts, output is not correct", CACHE_SIZE, testCache.getCacheSize());

            //Insert another value and verify that the 0th item is not there
            testCache.writeValueToCache("key_" + (CACHE_SIZE), Double.valueOf(CACHE_SIZE));
            Assert.assertTrue("Size limit not properly enforced. Least recently used item still present. ", !testCache.existsInCache("key_" + 0));

            //Overwrite head
            testCache.writeValueToCache("key_" + (CACHE_SIZE), Double.valueOf(CACHE_SIZE));
            Assert.assertTrue("Size limit not properly enforced. Least recently used item still present. ", testCache.existsInCache("key_" + 1));

            // This seems to cause a bug. A write at the size limit
            //tail's previous value not set properly (prob upon insert)
            testCache.writeValueToCache("key_" + (CACHE_SIZE + 1), Double.valueOf(CACHE_SIZE + 1));
            Assert.assertTrue("Size limit not properly enforced for cache size " + CACHE_SIZE + " Least recently used item still present. ", !testCache.existsInCache("key_" + 1));

            testCache.writeValueToCache("key_" + (CACHE_SIZE), 2* Double.valueOf(CACHE_SIZE)); //overwrite a value not at the head

            TestMultithreadedLRU.detectLoop(testCache, CACHE_SIZE);
            log.finest(testCache.toString());
        }
    }

    @Test
    public void testRewriteAtSizeLimit() {


    }

    @Test
    public void testResizeContainer() {

        LRUCache<String,Double> testCache = new LRUCache<String,Double>(TestBasicLRUCacheCorrectness.VALID_LRU_CACHE_SIZE);
    }
}
