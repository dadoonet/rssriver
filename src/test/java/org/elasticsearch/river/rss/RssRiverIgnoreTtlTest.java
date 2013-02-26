package org.elasticsearch.river.rss;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.junit.Test;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class RssRiverIgnoreTtlTest extends AbstractRssRiverSimpleTest {
    private static final int update_rate = 10 * 1000;
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
	 *   <li>http://www.lemonde.fr/rss/une.xml every 10 seconds and ignoring Ttl
	 * </ul>
	 */
	@Override
	public XContentBuilder rssRiver() throws Exception {
		// We create a rss feed on lemonde with a refresh every ten minutes
		// int updateRate = 10 * 60 * 1000;
		
		String url = "http://www.lemonde.fr/rss/une.xml";
		XContentBuilder xb = jsonBuilder()
				.startObject()
					.field("type", "rss")
					.startObject("rss")
						.startArray("feeds")
							.startObject()
								.field("url", url)
								.field("update_rate", update_rate)
                                .field("ignore_ttl", true)
							.endObject()
						.endArray()
					.endObject()
				.endObject();
		return xb;
	}
	

	@Test
	public void rss_update_rate_does_not_change() throws Exception {

        checkUpdateRateHelper(update_rate, true);

    }
}
