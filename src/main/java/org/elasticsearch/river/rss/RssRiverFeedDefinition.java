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
