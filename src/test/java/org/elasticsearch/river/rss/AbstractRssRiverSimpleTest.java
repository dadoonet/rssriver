/*
 * Licensed to David Pilato (the "Author") under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Author licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.river.rss;

import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.base.Predicate;
import org.elasticsearch.common.xcontent.support.XContentMapValues;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.river.RiverIndexName;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.equalTo;


public abstract class AbstractRssRiverSimpleTest extends AbstractRssRiverTest {

	protected void searchTestHelper(final String feedname) throws InterruptedException {
        // Make sure that everything is committed before testing.
        refresh();

        // Let's search for entries for darkreading
        logger.info("-->  checking that we have at least some docs in {}", indexName());
        assertThat(awaitBusy(new Predicate<Object>() {
            public boolean apply(Object obj) {
                SearchResponse searchResponse = client().prepareSearch(indexName())
                        .setQuery(QueryBuilders.queryString(feedname).defaultField("feedname")).execute().actionGet();
                return searchResponse.getHits().getTotalHits() > 1;
            }
        }, 5, TimeUnit.SECONDS), equalTo(true));
	}
	
	public void countTestHelper() throws Exception {
        // Make sure that everything is committed before testing.
        refresh();

		// Let's search for entries
        logger.info("-->  checking that we have at least some docs in {}", indexName());
        assertThat(awaitBusy(new Predicate<Object>() {
            public boolean apply(Object obj) {
                CountResponse response = client().prepareCount(indexName())
                        .setQuery(QueryBuilders.matchAllQuery()).execute().actionGet();
                return response.getCount() > 1;
            }
        }, 5, TimeUnit.SECONDS), equalTo(true));
	}

    public void checkUpdateRateHelper(int expectedRate, boolean equals) {
        GetResponse getResponse = client().prepareGet(RiverIndexName.Conf.DEFAULT_INDEX_NAME, indexName(), "_meta").execute().actionGet();
        Map<String, Object> source = getResponse.getSourceAsMap();

        boolean array = XContentMapValues.isArray(source.get("rss.feeds"));
        if (array) {
            ArrayList<Map<String, Object>> feeds = (ArrayList<Map<String, Object>>) source.get("rss.feeds");
            assertEquals(1, feeds.size());
            Map<String, Object> feed = feeds.get(0);

            int updateRate = XContentMapValues.nodeIntegerValue(feed.get("update_rate"));
            if (equals) {
                assertEquals(expectedRate * 1000, updateRate);
            } else {
                assertTrue(updateRate > expectedRate * 1000);
            }
        }
    }
}
