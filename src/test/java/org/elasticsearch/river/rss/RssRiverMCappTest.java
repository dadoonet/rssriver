package org.elasticsearch.river.rss;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

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
