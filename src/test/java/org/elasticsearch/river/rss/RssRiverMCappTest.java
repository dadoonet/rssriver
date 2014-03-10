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
	public void addFeeds(XContentBuilder xcb) {
        addRiver(xcb, "http://www.malwaredomains.com/wordpress/?feed=rss", "malwaredomains", 10);
        addRiver(xcb, "http://www.darkreading.com/rss/all.xml", "darkreading", 10);
	}
	
	@Test
	public void rss_feed_is_not_empty() throws Exception {
		countTestHelper();
		searchTestHelper("malwaredomains");
		searchTestHelper("darkreading");
	}
}
