package org.elasticsearch.river.rss;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author dadoonet (David Pilato)
 */
public class RssRiverMultipleFeedsTest extends AbstractRssRiverSimpleTest {
	/**
	 * Overriding mapping with french content
	 */
	@Override
	public XContentBuilder mapping() throws Exception {
		XContentBuilder xbMapping = 
			jsonBuilder()
				.startObject()
					.startObject("page")
						.startObject("properties")
							.startObject("source")
								.field("type", "string")
							.endObject()
							.startObject("title")
								.field("type", "string")
								.field("analyzer", "french")
							.endObject()
							.startObject("description")
								.field("type", "string")
								.field("analyzer", "french")
							.endObject()
							.startObject("author")
								.field("type", "string")
							.endObject()
							.startObject("link")
								.field("type", "string")
							.endObject()
						.endObject()
					.endObject()
				.endObject();
		return xbMapping;
	}

	/**
	 * 2 RSS feeds :
	 * <ul>
	 *   <li>http://www.lemonde.fr/rss/une.xml every 10 seconds
	 *   <li>http://rss.lefigaro.fr/lefigaro/laune every 15 seconds
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
								.field("name", "lemonde")
								.field("url", "http://www.lemonde.fr/rss/une.xml")
								.field("update_rate", 10 * 1000)
							.endObject()
							.startObject()
								.field("name", "lefigaro")
								.field("url", "http://rss.lefigaro.fr/lefigaro/laune")
								.field("update_rate", 15 * 1000)
							.endObject()
						.endArray()
					.endObject()
				.endObject();
		return xb;
	}
	
	@Test
	public void rss_feed_is_not_empty() throws Exception {
		countTestHelper();
		searchTestHelper("lemonde");
		searchTestHelper("lefigaro");
	}

	/**
	 * Uncomment this test if you need a long polling test
	 * @throws Exception
	 */
	@Test
	@Ignore
	public void testLongPolling() throws Exception {
		// Let's wait ten hours 
		Thread.sleep(10 * 60 * 60 * 1000);
	}
}
