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

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.common.base.Predicate;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.river.RiverIndexName;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.junit.Before;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.equalTo;

public abstract class AbstractRssRiverTest extends ElasticsearchIntegrationTest {

	/**
	 * Define a unique index name
	 * @return The unique index name (could be this.getClass().getSimpleName())
	 */
	protected String indexName() {
		return this.getClass().getSimpleName().toLowerCase();
	}
	
	/**
	 * Define a mapping if needed
	 * @return The mapping to use
	 */
	abstract public XContentBuilder mapping() throws Exception;
	
	/**
	 * Define the Rss River settings
	 * @return Rss River Settings
	 */
	abstract public XContentBuilder rssRiver() throws Exception;
	
    private void checkRiverIsStarted(final String riverName) throws InterruptedException {
        logger.info("-->  checking that river [{}] was created", riverName);
        assertThat(awaitBusy(new Predicate<Object>() {
            public boolean apply(Object obj) {
                GetResponse response = client().prepareGet(RiverIndexName.Conf.DEFAULT_INDEX_NAME, riverName, "_status").get();
                return response.isExists();
            }
        }, 5, TimeUnit.SECONDS), equalTo(true));
    }

    @Before
	public void setUp() throws Exception {
        super.setUp();

        createIndex(indexName());

		// If a mapping is defined, we will use it
		if (mapping() != null) {
			client().admin().indices()
			.preparePutMapping(indexName())
			.setType("page")
			.setSource(mapping())
			.execute().actionGet();
		}

		if (rssRiver() == null) throw new Exception("Subclasses must provide an rss setup...");
        index(RiverIndexName.Conf.DEFAULT_INDEX_NAME, indexName(), "_meta", rssRiver());
        checkRiverIsStarted(indexName());
	}

}
