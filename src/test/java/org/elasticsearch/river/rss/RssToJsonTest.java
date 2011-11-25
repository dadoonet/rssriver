package org.elasticsearch.river.rss;

import static org.junit.Assert.*;
import static org.elasticsearch.river.rss.RssToJson.toJson;

import java.io.IOException;

import com.sun.syndication.io.FeedException;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.junit.Test;

import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

public class RssToJsonTest {

    public static final String JSON = "{\"source\":null,\"title\":\"title\",\"author\":\"\",\"description\":\"desc\",\"link\":\"http://link.com/abc\",\"publishedDate\":\"2011-11-10T06:29:02.000Z\",\"source\":null,\"location\":{\"lon\":12.4839019775391,\"lat\":41.8947384616695}}";

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

}
