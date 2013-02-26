package org.elasticsearch.river.rss;

import com.sun.syndication.feed.rss.Channel;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RomeTest {

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
