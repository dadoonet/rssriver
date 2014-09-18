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

import com.rometools.modules.georss.GeoRSSModule;
import com.rometools.modules.georss.GeoRSSUtils;
import com.rometools.modules.georss.geometries.Position;
import com.rometools.modules.mediarss.MediaEntryModule;
import com.rometools.modules.mediarss.types.MediaContent;
import com.rometools.modules.mediarss.types.PlayerReference;
import com.rometools.modules.mediarss.types.UrlReference;
import com.rometools.rome.feed.synd.SyndCategory;
import com.rometools.rome.feed.synd.SyndContentImpl;
import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEntry;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.net.URI;

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
        public static final String CATEGORIES = "categories";

        public static final String LOCATION = "location";
        static public final class Location {
            public static final String LAT = "lat";
            public static final String LON = "lon";
        }

        public static final String ENCLOSURES = "enclosures";
        static public final class Enclosures {
            public static final String URL = "url";
            public static final String TYPE = "type";
            public static final String LENGTH = "length";
        }

        public static final String MEDIAS = "medias";
        static public final class Medias {
            public static final String TYPE = "type";
            public static final String REFERENCE = "reference";
            public static final String LANGUAGE = "language";
            public static final String TITLE = "title";
            public static final String DESCRIPTION = "description";
            public static final String DURATION = "duration";
            public static final String WIDTH = "width";
            public static final String HEIGHT = "height";
        }

        public static final String RAW = "raw";
        static public final class Raw {
            public static final String HTML = "html";
        }
    }

	public static XContentBuilder toJson(SyndEntry message, String riverName, String feedname, boolean raw) throws IOException {
        XContentBuilder out = jsonBuilder()
	    	.startObject()
	    		.field(Rss.FEEDNAME, feedname)
	    		.field(Rss.TITLE, message.getTitle())
	    		.field(Rss.AUTHOR, message.getAuthor())
	    		.field(Rss.DESCRIPTION, message.getDescription() != null ? message.getDescription().getValue() : null)
	    		.field(Rss.LINK, message.getLink())
	    		.field(Rss.PUBLISHED_DATE, message.getPublishedDate())
	    		.field(Rss.SOURCE, message.getSource());

        if (raw) {
            if (message.getContents() != null) {
                out.startObject(Rss.RAW);
                for (Object o : message.getContents()) {
                    if (o instanceof SyndContentImpl) {
                        SyndContentImpl content = (SyndContentImpl) o;
                        out.field(content.getType(), content.getValue());
                    }
                }
                out.endObject();
            }
        }

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

        if (message.getCategories() != null && message.getCategories().size() > 0) {
            out.startArray(Rss.CATEGORIES);
            for (Object oCategory : message.getCategories()) {
                if (oCategory instanceof SyndCategory) {
                    SyndCategory category = (SyndCategory) oCategory;
                    out.value(category.getName());
                }
            }
            out.endArray();
        }

        if (message.getEnclosures() != null && message.getEnclosures().size() > 0) {
            out.startArray(Rss.ENCLOSURES);
            for (Object oEnclosure : message.getEnclosures()) {
                if (oEnclosure instanceof SyndEnclosure) {
                    out.startObject();
                    SyndEnclosure enclosure = (SyndEnclosure) oEnclosure;
                    out.field(Rss.Enclosures.URL, enclosure.getUrl());
                    out.field(Rss.Enclosures.TYPE, enclosure.getType());
                    out.field(Rss.Enclosures.LENGTH, enclosure.getLength());
                    out.endObject();
                }
            }
            out.endArray();
        }

        MediaEntryModule mediaEntryModule = (MediaEntryModule) message.getModule(MediaEntryModule.URI);
        if (mediaEntryModule != null) {
            out.startArray(Rss.MEDIAS);
            for (MediaContent mediaContent : mediaEntryModule.getMediaContents()) {
                out.startObject();
                addFieldIfNotNull(out, Rss.Medias.TYPE, mediaContent.getType());

                if (mediaContent.getReference() != null) {
                    URI url = null;
                    if (mediaContent.getReference() instanceof PlayerReference) {
                        url = ((PlayerReference) mediaContent.getReference()).getUrl();
                    }
                    if (mediaContent.getReference() instanceof UrlReference) {
                        url = ((UrlReference) mediaContent.getReference()).getUrl();
                    }
                    if (url != null) {
                        addFieldIfNotNull(out, Rss.Medias.REFERENCE, url.toString());
                    }
                }

                addFieldIfNotNull(out, Rss.Medias.LANGUAGE, mediaContent.getLanguage());
                addFieldIfNotNull(out, Rss.Medias.TITLE, mediaContent.getMetadata().getTitle());
                addFieldIfNotNull(out, Rss.Medias.DESCRIPTION, mediaContent.getMetadata().getDescription());
                addFieldIfNotNull(out, Rss.Medias.DURATION, mediaContent.getDuration());
                addFieldIfNotNull(out, Rss.Medias.WIDTH, mediaContent.getWidth());
                addFieldIfNotNull(out, Rss.Medias.HEIGHT, mediaContent.getHeight());
                out.endObject();
            }
            out.endArray();
        }

        if (riverName != null) {
            out.field("river", riverName);
        }

        return out.endObject();
	}

    private static void addFieldIfNotNull(XContentBuilder xcb, String fieldName, String content) throws IOException {
        if (content != null) {
            xcb.field(fieldName, content);
        }
    }

    private static void addFieldIfNotNull(XContentBuilder xcb, String fieldName, Integer content) throws IOException {
        if (content != null) {
            xcb.field(fieldName, content);
        }
    }

    private static void addFieldIfNotNull(XContentBuilder xcb, String fieldName, Long content) throws IOException {
        if (content != null) {
            xcb.field(fieldName, content);
        }
    }

    /**
     * Build the mapping for RSS content
     * @param type elasticsearch type you will use
     * @return a mapping
     * @throws Exception
     */
    public static XContentBuilder buildRssMapping(String type, boolean raw) throws Exception {
        XContentBuilder xbMapping = jsonBuilder().prettyPrint().startObject();

        // Type
        xbMapping.startObject(type);

        xbMapping.startObject("properties");

        // feed document
        addNotAnalyzedString(xbMapping, Rss.FEEDNAME);
        addAnalyzedString(xbMapping, Rss.TITLE);
        addAnalyzedString(xbMapping, Rss.AUTHOR);
        addAnalyzedString(xbMapping, Rss.DESCRIPTION);
        addNotIndexedString(xbMapping, Rss.LINK);
        addAnalyzedString(xbMapping, Rss.SOURCE);
        addDate(xbMapping, Rss.PUBLISHED_DATE);
        addGeopoint(xbMapping, Rss.LOCATION);
        addNotAnalyzedString(xbMapping, Rss.CATEGORIES);

        // Enclosures
        xbMapping.startObject(Rss.ENCLOSURES).startObject("properties");
        addNotIndexedString(xbMapping, Rss.Enclosures.URL);
        addNotAnalyzedString(xbMapping, Rss.Enclosures.TYPE);
        addNotIndexedLong(xbMapping, Rss.Enclosures.LENGTH);
        xbMapping.endObject().endObject(); // End Enclosures

        // Medias
        xbMapping.startObject(Rss.MEDIAS).startObject("properties");
        addNotAnalyzedString(xbMapping, Rss.Medias.TYPE);
        addNotIndexedString(xbMapping, Rss.Medias.REFERENCE);
        addNotAnalyzedString(xbMapping, Rss.Medias.LANGUAGE);
        addAnalyzedString(xbMapping, Rss.Medias.TITLE);
        addAnalyzedString(xbMapping, Rss.Medias.DESCRIPTION);
        addNotIndexedLong(xbMapping, Rss.Medias.DURATION);
        addNotIndexedLong(xbMapping, Rss.Medias.WIDTH);
        addNotIndexedLong(xbMapping, Rss.Medias.HEIGHT);
        xbMapping.endObject().endObject(); // End Medias

        // Raw content:encoded
        if (raw) {
            xbMapping.startObject(Rss.RAW).startObject("properties");
            addNotIndexedString(xbMapping, Rss.Raw.HTML);
            xbMapping.endObject().endObject(); // End Raw
        }

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

    private static void addNotIndexedLong(XContentBuilder xcb, String fieldName) throws IOException {
        xcb.startObject(fieldName)
                .field("type", "long")
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
