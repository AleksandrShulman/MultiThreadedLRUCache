import org.junit.Assert;
import org.junit.Test;

/**
 * Created by aleks on 8/6/15.
 */
public class TestResizeCache {

    @Test
    public void testSizeCorrect() {

        final int ORIGINAL_SIZE = 20;
        final int SMALLER_SIZE = 9;
        final int REMOVED_NODES = ORIGINAL_SIZE - SMALLER_SIZE;
        final int EXPECTED_INDEX_HEAD_NODE = ORIGINAL_SIZE-1;

        //remove 0 nodes, tail has index 0. Remove 1 node, tail has index 1
        final int EXPECTED_INDEX_TAIL_NODE = REMOVED_NODES;

        LRUCache<String,Double> testCache = new LRUCache<String,Double>(ORIGINAL_SIZE);


        for (int i=0; i<ORIGINAL_SIZE; i++) {

            testCache.writeValueToCache("KEY_"+ i, Double.valueOf(i));
        }

        Assert.assertEquals("Wrong count after inserting known set of items", ORIGINAL_SIZE, testCache.getCacheSize());

        testCache.resizeContainer(SMALLER_SIZE);

        Assert.assertEquals("Wrong count after decreasing container size", SMALLER_SIZE, testCache.getCacheSize());

        Assert.assertTrue("Last value entered not in cache", testCache.existsInCache("KEY_" + EXPECTED_INDEX_HEAD_NODE));
        Assert.assertTrue("First value inserted still in cache", !testCache.existsInCache("KEY_" + 0));

        //Test more stringent conditions
        Assert.assertTrue("Edge value not present though it should be.", testCache.existsInCache("KEY_" + REMOVED_NODES));
        Assert.assertTrue("Edge value present though it should not be", !testCache.existsInCache("KEY_" + (REMOVED_NODES-1)));

        //Verify that the head and tail are correct
        Assert.assertTrue("Head node is not the correct value",
                testCache.getOrderedList().getHead().getEntry().getKey().equals("KEY_" + EXPECTED_INDEX_HEAD_NODE));

        Assert.assertEquals("Tail node not the one expected.", "KEY_" + EXPECTED_INDEX_TAIL_NODE,
                testCache.getOrderedList().getTail().getEntry().getKey());

        //Go one step further and really verify that the last item really is the last one!
        Assert.assertEquals("There is something beyond the tail node!!!", null, testCache.getOrderedList().getTail().getNext());
    }

    @Test
    public void testResizeTo1() {

        final int ORIGINAL_SIZE = 20;
        final int SMALLER_SIZE = 1;

        LRUCache<String,Double> testCache = new LRUCache<String,Double>(ORIGINAL_SIZE);


        for (int i=0; i<ORIGINAL_SIZE; i++) {

            testCache.writeValueToCache("KEY_"+ i, Double.valueOf(i));
        }

        Assert.assertEquals("Wrong count after inserting known set of items", ORIGINAL_SIZE, testCache.getCacheSize());

        testCache.resizeContainer(SMALLER_SIZE);

        Assert.assertEquals("Wrong count after decreasing container size", SMALLER_SIZE, testCache.getCacheSize());
        Assert.assertEquals(testCache.getOrderedList().getTail(), testCache.getOrderedList().getHead());
        Assert.assertSame(testCache.getOrderedList().getTail(),testCache.getOrderedList().getHead());
    }

    @Test
    public void testResizeTo0() {

        final int ORIGINAL_SIZE = 20;
        final int SMALLER_SIZE = 0;

        LRUCache<String,Double> testCache = new LRUCache<String,Double>(ORIGINAL_SIZE);


        for (int i=0; i<ORIGINAL_SIZE; i++) {

            testCache.writeValueToCache("KEY_"+ i, Double.valueOf(i));
        }

        Assert.assertEquals("Wrong count after inserting known set of items", ORIGINAL_SIZE, testCache.getCacheSize());

        try {
            testCache.resizeContainer(SMALLER_SIZE);
            Assert.fail("Resizing to invalid size did not throw a runtime exception");
        } catch (IllegalStateException ise) {

            Assert.assertTrue(ise.getMessage().contains(LRUCache.INVALID_RESIZE_ERROR_MSG));
        }
    }
}
