package org.elasticsearch.river.rss;

import static org.junit.Assert.*;

import java.net.URL;

import org.junit.Test;

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

public class RomeTest {

	@Test
	public void test() throws Exception {
		String url = "http://www.lemonde.fr/rss/une.xml";
		URL feedUrl = new URL(url);

		SyndFeedInput input = new SyndFeedInput();
		SyndFeed feed = input.build(new XmlReader(feedUrl));

		assertNotNull(feed);
		assertFalse(feed.getEntries().isEmpty());
	}
}
