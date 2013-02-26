package org.elasticsearch.river.rss;

/**
 * Define an RSS Feed with source (aka short name), url and updateRate attributes
 * @author dadoonet (David Pilato)
 * @since 0.0.5
 */
public class RssRiverFeedDefinition {
	private String feedname;
	private String url;
	private int updateRate;
    private boolean ignoreTtl = false;
	
	public RssRiverFeedDefinition() {
	}
	
	public RssRiverFeedDefinition(String feedname, String url, int updateRate, boolean ignoreTtl) {
		this.feedname = feedname;
		this.url = url;
		this.updateRate = updateRate;
        this.ignoreTtl = ignoreTtl;
	}
	
	public String getFeedname() {
		return feedname;
	}
	
	public void setFeedname(String feedname) {
		this.feedname = feedname;
	}
	
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public int getUpdateRate() {
		return updateRate;
	}

	public void setUpdateRate(int updateRate) {
		this.updateRate = updateRate;
	}

    public boolean isIgnoreTtl() {
        return ignoreTtl;
    }

    public void setIgnoreTtl(boolean ignoreTtl) {
        this.ignoreTtl = ignoreTtl;
    }
}
