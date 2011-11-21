/*
 * Licensed to Elastic Search and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Elastic Search licenses this
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

import static org.elasticsearch.client.Requests.indexRequest;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.river.rss.RssToJson.toJson;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Map;

import com.sun.syndication.feed.synd.SyndEntry;
import org.elasticsearch.ExceptionsHelper;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.action.bulk.BulkRequestBuilder;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.common.UUID;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.joda.time.format.ISODateTimeFormat;
import org.elasticsearch.common.util.concurrent.EsExecutors;
import org.elasticsearch.common.xcontent.support.XContentMapValues;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.elasticsearch.river.AbstractRiverComponent;
import org.elasticsearch.river.River;
import org.elasticsearch.river.RiverName;
import org.elasticsearch.river.RiverSettings;

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

/**
 * @author dadoonet (David Pilato)
 */
public class RssRiver extends AbstractRiverComponent implements River {

	private final Client client;

	private final String url;

	private final String indexName;

	private final String typeName;

	private volatile Thread thread;

	private volatile boolean closed = false;

	private final int updateRate;

	@SuppressWarnings({ "unchecked" })
	@Inject
	public RssRiver(RiverName riverName, RiverSettings settings, Client client)
			throws MalformedURLException {
		super(riverName, settings);
		this.client = client;

		if (settings.settings().containsKey("rss")) {
			Map<String, Object> rssSettings = (Map<String, Object>) settings.settings().get("rss");
			url = XContentMapValues.nodeStringValue(rssSettings.get("url"), null);
			updateRate  = XContentMapValues.nodeIntegerValue(rssSettings.get("update_rate"), 15 * 60 * 1000);
		} else {
			url = "http://www.lemonde.fr/rss/une.xml";
			logger.warn("You didn't define the rss url. Switching to defaults : [{}]", url);
			updateRate = 15 * 60 * 1000;
		}

		if (logger.isInfoEnabled()) logger.info("creating rss stream river for [{}]", url);
		
		if (settings.settings().containsKey("index")) {
			Map<String, Object> indexSettings = (Map<String, Object>) settings
					.settings().get("index");
			indexName = XContentMapValues.nodeStringValue(
					indexSettings.get("index"), riverName.name());
			typeName = XContentMapValues.nodeStringValue(
					indexSettings.get("type"), "page");
		} else {
			indexName = riverName.name();
			typeName = "page";
		}
	}

	@Override
	public void start() {
		if (logger.isInfoEnabled()) logger.info("Starting rss stream");
		try {
			client.admin().indices().prepareCreate(indexName).execute()
					.actionGet();
		} catch (Exception e) {
			if (ExceptionsHelper.unwrapCause(e) instanceof IndexAlreadyExistsException) {
				// that's fine
			} else if (ExceptionsHelper.unwrapCause(e) instanceof ClusterBlockException) {
				// ok, not recovered yet..., lets start indexing and hope we
				// recover by the first bulk
				// TODO: a smarter logic can be to register for cluster event
				// listener here, and only start sampling when the block is
				// removed...
			} else {
				logger.warn("failed to create index [{}], disabling river...",
						e, indexName);
				return;
			}
		}
		thread = EsExecutors.daemonThreadFactory(settings.globalSettings(),
				"rss_slurper").newThread(new RSSParser());
		thread.start();
	}

	@Override
	public void close() {
		if (logger.isInfoEnabled()) logger.info("Closing rss river");
		closed = true;
		if (thread != null) {
			thread.interrupt();
		}
	}

	
	private SyndFeed getFeed(String url) {
		try {
			URL feedUrl = new URL(url);
			SyndFeedInput input = new SyndFeedInput();
			SyndFeed feed = input.build(new XmlReader(feedUrl));
			return feed;
		} catch (MalformedURLException e) {
			logger.error("RSS Url is incorrect : [{}].", url);
		} catch (IllegalArgumentException e) {
			logger.error("Feed from [{}] is incorrect.", url);
		} catch (FeedException e) {
			logger.error("Can not parse feed from [{}].", url);
		} catch (IOException e) {
			logger.error("Can not read feed from [{}].", url);
		}
		
		return null;
	}
	
	private class RSSParser implements Runnable {

		@SuppressWarnings("unchecked")
		@Override
		public void run() {
            while (true) {
				if (closed) {
					return;
				}
				
				// Let's call the Rss flow
				SyndFeed feed = getFeed(url);
                if (feed != null) {
                    Date feedDate = feed.getPublishedDate();
                    logger.debug("Feed publish date is {}", feedDate);

                    Date lastDate = getLastDateFromRiver();
                    // Comparing dates to see if we have something to do or not
                    if (lastDate == null || (feedDate != null && feedDate.after(lastDate))) {
                        // We have to send results to ES
                        if (logger.isDebugEnabled()) logger.debug("Feed is updated : {}", feed);

                        BulkRequestBuilder bulk = client.prepareBulk();
                        try {
                            // We have now to send each feed to ES
                            for (SyndEntry message : (Iterable<SyndEntry>) feed.getEntries()) {
                                String description = "";
                                if (message.getDescription() != null) {
                                    description = message.getDescription().getValue();
                                }

                                // Let's define the rule for UUID generation
                                String id = UUID.nameUUIDFromBytes(description.getBytes()).toString();

                                // Let's look if object already exists
                                GetResponse oldMessage = client.prepareGet(indexName, typeName, id).execute().actionGet();
                                if (!oldMessage.exists()) {
                                    bulk.add(indexRequest(indexName).type(typeName).id(id).source(toJson(message, riverName.getName())));

                                    if (logger.isDebugEnabled()) logger.debug("FeedMessage is updated : {}", message);
                                } else {
                                    if (logger.isTraceEnabled()) logger.trace("FeedMessage {} already exist. Ignoring", id);
                                }
                            }

                            if (logger.isTraceEnabled()) {
                                logger.trace("processing [_seq  ]: [{}]/[{}]/[{}], last_seq [{}]", indexName, riverName.name(), "_lastupdate", feedDate);
                            }
                            // We store the lastupdate date
                            bulk.add(indexRequest("_river").type(riverName.name()).id("_lastupdate")
                                    .source(jsonBuilder().startObject().startObject("rss").field("_lastupdate", feedDate).endObject().endObject()));
                        } catch (IOException e) {
                            logger.warn("failed to add feed message entry to bulk indexing");
                        }

                        try {
                            BulkResponse response = bulk.execute().actionGet();
                            if (response.hasFailures()) {
                                // TODO write to exception queue?
                                logger.warn("failed to execute" + response.buildFailureMessage());
                            }
                        } catch (Exception e) {
                            logger.warn("failed to execute bulk", e);
                        }

                    } else {
                        // Nothing new... Just relax !
                        if (logger.isDebugEnabled()) logger.debug("Nothing new in the feed... Relaxing...");
                    }
                }
				try {
					if (logger.isDebugEnabled()) logger.debug("Rss river is going to sleep for {} ms", updateRate);
					Thread.sleep(updateRate);
				} catch (InterruptedException e1) {
				}
			}
		}

        private Date getLastDateFromRiver() {
            Date lastDate = null;
            try {
                // Do something
                if (logger.isDebugEnabled()) logger.debug("Starting to parse RSS feed");
                client.admin().indices().prepareRefresh("_river").execute().actionGet();
                GetResponse lastSeqGetResponse =
                        client.prepareGet("_river", riverName().name(), "_lastupdate").execute().actionGet();
                if (lastSeqGetResponse.exists()) {
                    Map<String, Object> rssState = (Map<String, Object>) lastSeqGetResponse.sourceAsMap().get("rss");

                    if (rssState != null) {
                        Object lastupdate = rssState.get("_lastupdate");
                        if (lastupdate != null) {
                            String strLastDate = lastupdate.toString();
                            lastDate = ISODateTimeFormat.dateOptionalTimeParser().parseDateTime(strLastDate).toDate();
                        }
                    }
                } else {
                    // First call
                    if (logger.isDebugEnabled()) logger.debug("_lastupdate doesn't exist");
                }
            } catch (Exception e) {
                logger.warn("failed to get _lastupdate, throttling....", e);
            }
            return lastDate;
        }
    }
}
