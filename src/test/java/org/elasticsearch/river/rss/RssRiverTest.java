package org.elasticsearch.river.rss;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.junit.Test;

public class RssRiverTest extends AbstractRssRiverSimpleTest {

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
	 * 1 RSS feed :
	 * <ul>
	 *   <li>http://www.lemonde.fr/rss/une.xml every 10 seconds
	 * </ul>
	 */
	@Override
	public XContentBuilder rssRiver() throws Exception {
		// We create a rss feed on lemonde with a refresh every ten minutes
		// int updateRate = 10 * 60 * 1000;
		
		String url = "http://www.lemonde.fr/rss/une.xml";
		int updateRate = 10 * 1000;
		XContentBuilder xb = jsonBuilder()
				.startObject()
					.field("type", "rss")
					.startObject("rss")
						.startArray("feeds")
							.startObject()
								.field("url", url)
								.field("update_rate", updateRate)
							.endObject()
						.endArray()
					.endObject()
				.endObject();
		return xb;
	}
	

	@Test
	public void rss_feed_is_not_empty() throws Exception {
		countTestHelper();
	}
}
