package org.elasticsearch.river.rss;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;

import org.elasticsearch.common.xcontent.XContentBuilder;

import com.sun.syndication.feed.synd.SyndEntryImpl;

public class RssToJson {
	public static XContentBuilder toJson(SyndEntryImpl message) throws IOException {
		return jsonBuilder()
	    	.startObject()
	    		.field("title", message.getTitle())
	    		.field("author", message.getAuthor())
	    		.field("description", message.getDescription() != null ? message.getDescription().getValue() : null)
	    		.field("link", message.getLink())
	    		.field("publishedDate", message.getPublishedDate())
	    		.field("source", message.getSource())
	    	.endObject();
	}
}
