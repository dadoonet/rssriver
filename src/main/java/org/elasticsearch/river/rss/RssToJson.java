package org.elasticsearch.river.rss;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.sun.syndication.feed.module.georss.GeoRSSModule;
import com.sun.syndication.feed.module.georss.GeoRSSUtils;
import com.sun.syndication.feed.module.georss.geometries.Position;
import org.elasticsearch.common.xcontent.XContentBuilder;

import com.sun.syndication.feed.synd.SyndEntryImpl;
import org.elasticsearch.river.RiverName;

public class RssToJson {
	public static XContentBuilder toJson(SyndEntryImpl message, String riverName) throws IOException {
        XContentBuilder out = jsonBuilder()
	    	.startObject()
	    		.field("title", message.getTitle())
	    		.field("author", message.getAuthor())
	    		.field("description", message.getDescription() != null ? message.getDescription().getValue() : null)
	    		.field("link", message.getLink())
	    		.field("publishedDate", message.getPublishedDate())
	    		.field("source", message.getSource());

        final Map<String, Object> latitude = getPosition(message);
        if (latitude.size() > 0) {
            out.field("location", latitude);
        }
        if (riverName != null) {
            out.field("river");
        }
        return out.endObject();
	}

    private static Map<String, Object> getPosition(SyndEntryImpl message) {
        GeoRSSModule geoRSSModule = GeoRSSUtils.getGeoRSS(message);
        final Map<String, Object> latitude = new HashMap<String, Object>();
        if (geoRSSModule != null) {
            final Position position = geoRSSModule.getPosition();
            if (position != null) {
                latitude.put("lat", position.getLatitude());
                latitude.put("lon", position.getLongitude());
            }
        }
        return latitude;
    }
}
