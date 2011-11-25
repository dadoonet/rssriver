package org.elasticsearch.river.rss;

/**
 * Define an RSS Feed with url and updateRate attributes
 * @author dadoonet (David Pilato)
 * @since 0.0.5
 */
public class RssRiverFeedDefinition {
	private String url;
	private int updateRate;
	
	public RssRiverFeedDefinition() {
	}
	
	public RssRiverFeedDefinition(String url, int updateRate) {
		super();
		this.url = url;
		this.updateRate = updateRate;
	}
	
	/**
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}
	/**
	 * @param url the url to set
	 */
	public void setUrl(String url) {
		this.url = url;
	}
	/**
	 * @return the updateRate
	 */
	public int getUpdateRate() {
		return updateRate;
	}
	/**
	 * @param updateRate the updateRate to set
	 */
	public void setUpdateRate(int updateRate) {
		this.updateRate = updateRate;
	}
	
	
}
