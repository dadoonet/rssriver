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

package org.elasticsearch.river.rss.unit;

import com.sun.syndication.feed.module.georss.GeoRSSModule;
import com.sun.syndication.feed.module.georss.GeoRSSUtils;
import com.sun.syndication.feed.module.georss.geometries.Position;
import com.sun.syndication.feed.rss.Channel;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.junit.Test;

import static org.hamcrest.Matchers.*;

public class RomeTest extends ElasticsearchTestCase {

	@Test
	public void testLeMonde() throws Exception {
		SyndFeedInput input = new SyndFeedInput();
        input.setPreserveWireFeed(true);
		SyndFeed feed = input.build(new XmlReader(getClass().getResource("/lemonde/rss.xml")));

        assertThat(feed, notNullValue());
        assertThat(feed.getEntries().isEmpty(), equalTo(false));

        assertThat(feed.originalWireFeed(), notNullValue());
        assertThat(feed.originalWireFeed(), instanceOf(Channel.class));

        Channel channel = (Channel) feed.originalWireFeed();
        assertThat(channel.getTtl(), equalTo(15));
	}

    @Test
    public void testGeoLoc() throws Exception {
        SyndFeedInput input = new SyndFeedInput();
        input.setPreserveWireFeed(true);
        SyndFeed feed = input.build(new XmlReader(getClass().getResource("/reuters/rss.xml")));

        assertThat(feed, notNullValue());
        assertThat(feed.getEntries().isEmpty(), equalTo(false));

        for (Object o : feed.getEntries()) {
            assertThat(o, instanceOf(SyndEntryImpl.class));
            SyndEntryImpl entry = (SyndEntryImpl) o;

            GeoRSSModule geoRSSModule = GeoRSSUtils.getGeoRSS(entry);
            assertThat(geoRSSModule, notNullValue());

            Position position = geoRSSModule.getPosition();
            assertThat(position, notNullValue());

            assertThat(position.getLatitude(), equalTo(41.8947384616695));
            assertThat(position.getLongitude(), equalTo(12.4839019775391));
        }
    }
}
