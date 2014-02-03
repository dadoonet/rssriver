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

import com.sun.syndication.feed.module.georss.GeoRSSModule;
import com.sun.syndication.feed.module.georss.GeoRSSUtils;
import com.sun.syndication.feed.module.georss.geometries.Position;
import com.sun.syndication.feed.synd.SyndEntry;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class RssToJson {
    static public final class Rss {
        public static final String FEEDNAME = "feedname";
        public static final String AUTHOR = "author";
        public static final String TITLE = "title";
        public static final String DESCRIPTION = "description";
        public static final String LINK = "link";
        public static final String PUBLISHED_DATE = "publishedDate";
        public static final String SOURCE = "source";

        public static final String LOCATION = "location";
        static public final class Location {
            public static final String LAT = "lat";
            public static final String LON = "lon";
        }
    }

	public static XContentBuilder toJson(SyndEntry message, String riverName, String feedname) throws IOException {
        XContentBuilder out = jsonBuilder()
	    	.startObject()
	    		.field(Rss.FEEDNAME, feedname)
	    		.field(Rss.TITLE, message.getTitle())
	    		.field(Rss.AUTHOR, message.getAuthor())
	    		.field(Rss.DESCRIPTION, message.getDescription() != null ? message.getDescription().getValue() : null)
	    		.field(Rss.LINK, message.getLink())
	    		.field(Rss.PUBLISHED_DATE, message.getPublishedDate())
	    		.field(Rss.SOURCE, message.getSource());

        GeoRSSModule geoRSSModule = GeoRSSUtils.getGeoRSS(message);
        if (geoRSSModule != null) {
            final Position position = geoRSSModule.getPosition();
            if (position != null) {
                out.startObject(Rss.LOCATION);
                out.field(Rss.Location.LAT, position.getLatitude());
                out.field(Rss.Location.LON, position.getLongitude());
                out.endObject();
            }
        }

        if (riverName != null) {
            out.field("river", riverName);
        }

        return out.endObject();
	}

    /**
     * Build the mapping for RSS content
     * @param type elasticsearch type you will use
     * @return a mapping
     * @throws Exception
     */
    public static XContentBuilder buildRssMapping(String type) throws Exception {
        XContentBuilder xbMapping = jsonBuilder().prettyPrint().startObject();

        // Type
        xbMapping.startObject(type);

        xbMapping.startObject("properties");

        // Doc content
        addNotAnalyzedString(xbMapping, Rss.FEEDNAME);
        addAnalyzedString(xbMapping, Rss.TITLE);
        addAnalyzedString(xbMapping, Rss.AUTHOR);
        addAnalyzedString(xbMapping, Rss.DESCRIPTION);
        addNotIndexedString(xbMapping, Rss.LINK);
        addAnalyzedString(xbMapping, Rss.SOURCE);
        addDate(xbMapping, Rss.PUBLISHED_DATE);
        addGeopoint(xbMapping, Rss.LOCATION);

        xbMapping.endObject().endObject().endObject(); // End Type
        return xbMapping;
    }

    private static void addAnalyzedString(XContentBuilder xcb, String fieldName) throws IOException {
        xcb.startObject(fieldName)
                .field("type", "string")
                .endObject();
    }

    private static void addNotAnalyzedString(XContentBuilder xcb, String fieldName) throws IOException {
        xcb.startObject(fieldName)
                .field("type", "string")
                .field("index", "not_analyzed")
                .endObject();
    }

    private static void addNotIndexedString(XContentBuilder xcb, String fieldName) throws IOException {
        xcb.startObject(fieldName)
                .field("type", "string")
                .field("index", "no")
                .endObject();
    }

    private static void addDate(XContentBuilder xcb, String fieldName) throws IOException {
        xcb.startObject(fieldName)
                .field("type", "date")
                .field("format", "dateOptionalTime")
                .field("store", "yes")
                .endObject();
    }

    private static void addGeopoint(XContentBuilder xcb, String fieldName) throws IOException {
        xcb.startObject(fieldName)
                .field("type", "geo_point")
                .endObject();
    }
}
