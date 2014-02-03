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

import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.junit.Test;

import java.io.IOException;

import static org.elasticsearch.river.rss.RssToJson.toJson;
import static org.hamcrest.CoreMatchers.notNullValue;

public class RssToJsonTest extends ElasticsearchTestCase {

    public static final String JSON = "{\"feedname\":null,\"title\":\"title\",\"author\":\"\",\"description\":\"desc\",\"link\":\"http://link.com/abc\",\"publishedDate\":\"2011-11-10T06:29:02.000Z\",\"source\":null,\"location\":{\"lat\":41.8947384616695,\"lon\":12.4839019775391},\"categories\":[\"worldNews\"]}";

    @Test /* this test should be moved somewhere else */
	public void shouldParseRss() throws Exception {
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader(getClass().getResource("/rss.xml")));

        assertTrue(feed.getEntries().size() > 0);
        for (Object o : feed.getEntries()) {
            SyndEntryImpl message = (SyndEntryImpl) o;
            XContentBuilder xcb = toJson(message, null, null);
            assertNotNull(xcb);
        }
	}

    @Test
    public void shouldParseRssGeoInformation() throws Exception {
        final SyndEntryImpl entry = buildEntry();
        final XContentBuilder xContentBuilder = RssToJson.toJson(entry, null, null);
        assertEquals(JSON, xContentBuilder.string());
    }

    private SyndEntryImpl buildEntry() throws FeedException, IOException {
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader(getClass().getResource("/rss.xml")));
        return (SyndEntryImpl) feed.getEntries().get(0);
    }

    @Test
    public void mappingShouldNotfail() throws Exception {
        XContentBuilder page = RssToJson.buildRssMapping("page");
        assertThat(page, notNullValue());
        logger.info("mapping is: {}", page.string());
    }

}
