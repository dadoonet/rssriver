package org.elasticsearch.river.rss;

import static org.junit.Assert.*;
import static org.elasticsearch.river.rss.RssToJson.toJson;

import java.net.URL;
import java.util.Iterator;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.junit.Test;

import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

public class RssToJsonTest {

	@Test
	public void testToJson() throws Exception {
		String url = "http://www.lemonde.fr/rss/une.xml";
		URL feedUrl = new URL(url);

		SyndFeedInput input = new SyndFeedInput();
		SyndFeed feed = input.build(new XmlReader(feedUrl));

		assertNotNull(feed);
		assertFalse(feed.getEntries().isEmpty());
		
    	for (Iterator<SyndEntryImpl> iterator = feed.getEntries().iterator(); iterator.hasNext();) {
    		SyndEntryImpl message = (SyndEntryImpl) iterator.next();
			XContentBuilder xcb = toJson(message);
			assertNotNull(xcb);
			
			System.out.println(xcb.string());
		}
	}

}
