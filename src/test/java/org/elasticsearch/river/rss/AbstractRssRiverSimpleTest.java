package org.elasticsearch.river.rss;

import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.xcontent.support.XContentMapValues;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;


public abstract class AbstractRssRiverSimpleTest extends AbstractRssRiverTest {

	/**
	 * We wait for 5 seconds before each test
	 */
	@Override
	public long waitingTime() throws Exception {
		return 5;
	}

	protected void searchTestHelper(String feedname) {
		// Let's search for entries for darkreading
		SearchResponse searchResponse = node.client().prepareSearch(indexName())
				.setQuery(QueryBuilders.fieldQuery("feedname", feedname)).execute().actionGet();
		Assert.assertTrue("We should have at least one doc for " + feedname + "...", searchResponse.getHits().getTotalHits() > 1);
	}
	
	public void countTestHelper() throws Exception {
		// Let's search for entries
		CountResponse response = node.client().prepareCount(indexName())
				.setQuery(QueryBuilders.matchAllQuery()).execute().actionGet();
		Assert.assertTrue("We should have at least one doc...", response.getCount() > 1);
	}

    public void checkUpdateRateHelper(int expectedRate, boolean equals) {
        GetResponse getResponse = node.client().prepareGet("_river", indexName(), "_meta").execute().actionGet();
        Map<String, Object> source = getResponse.getSourceAsMap();

        boolean array = XContentMapValues.isArray(source.get("rss.feeds"));
        if (array) {
            ArrayList<Map<String, Object>> feeds = (ArrayList<Map<String, Object>>) source.get("rss.feeds");
            assertEquals(1, feeds.size());
            Map<String, Object> feed = feeds.get(0);

            int updateRate = XContentMapValues.nodeIntegerValue(feed.get("update_rate"));
            if (equals) {
                assertEquals(expectedRate, updateRate);
            } else {
                assertTrue(updateRate > expectedRate);
            }
        }
    }
}
