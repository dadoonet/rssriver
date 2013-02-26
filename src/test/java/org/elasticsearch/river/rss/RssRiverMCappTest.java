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

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.junit.Test;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class RssRiverMCappTest extends AbstractRssRiverSimpleTest {

	/**
	 * Using default mapping
	 */
	@Override
	public XContentBuilder mapping() throws Exception {
		return null;
	}

	/**
	 * 2 RSS feeds :
	 * <ul>
	 *   <li>http://www.malwaredomains.com/wordpress/?feed=rss every 10 seconds
	 *   <li>http://www.darkreading.com/rss/all.xml every 10 seconds
	 * </ul>
	 */
	@Override
	public XContentBuilder rssRiver() throws Exception {
		XContentBuilder xb = jsonBuilder()
				.startObject()
					.field("type", "rss")
					.startObject("rss")
						.startArray("feeds")
							.startObject()
								.field("name", "malwaredomains")
								.field("url", "http://www.malwaredomains.com/wordpress/?feed=rss")
								.field("update_rate", 10 * 1000)
							.endObject()
							.startObject()
								.field("name", "darkreading")
								.field("url", "http://www.darkreading.com/rss/all.xml")
								.field("update_rate", 10 * 1000)
							.endObject()
						.endArray()
					.endObject()
				.endObject();
		return xb;
	}
	
	@Test
	public void rss_feed_is_not_empty() throws Exception {
		countTestHelper();
		searchTestHelper("malwaredomains");
		searchTestHelper("darkreading");
	}
}
