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

import com.sun.syndication.feed.rss.Channel;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.junit.Test;

import java.net.URL;

public class RomeTest extends ElasticsearchTestCase {

	@Test
	public void test() throws Exception {
		String url = "http://www.lemonde.fr/rss/une.xml";
		URL feedUrl = new URL(url);

		SyndFeedInput input = new SyndFeedInput();
        input.setPreserveWireFeed(true);
		SyndFeed feed = input.build(new XmlReader(feedUrl));

		assertNotNull(feed);
		assertFalse(feed.getEntries().isEmpty());

        assertNotNull(feed.originalWireFeed());
        assertTrue(feed.originalWireFeed() instanceof Channel);

        Channel channel = (Channel) feed.originalWireFeed();
        assertTrue(channel.getTtl() >= 0);
	}
}
