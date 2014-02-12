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

import com.sun.syndication.feed.rss.Channel;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

import org.elasticsearch.ExceptionsHelper;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.bulk.*;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.joda.time.format.ISODateTimeFormat;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.util.concurrent.EsExecutors;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.support.XContentMapValues;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.elasticsearch.river.AbstractRiverComponent;
import org.elasticsearch.river.River;
import org.elasticsearch.river.RiverName;
import org.elasticsearch.river.RiverSettings;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.elasticsearch.client.Requests.indexRequest;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.river.rss.RssToJson.toJson;

/**
 * @author dadoonet (David Pilato)
 */
public class RssRiver extends AbstractRiverComponent implements River {

	private final Client client;

	private final String indexName;

	private final String typeName;

    private final int bulkSize;
    private final int maxConcurrentBulk;
    private final TimeValue bulkFlushInterval;

    private volatile BulkProcessor bulkProcessor;

    private volatile ArrayList<Thread> threads;

	private volatile boolean closed = false;

	private final ArrayList<RssRiverFeedDefinition> feedsDefinition;

	@SuppressWarnings({ "unchecked" })
	@Inject
	public RssRiver(RiverName riverName, RiverSettings settings, Client client)
			throws MalformedURLException {
		super(riverName, settings);
		this.client = client;
		if (settings.settings().containsKey("rss")) {
			Map<String, Object> rssSettings = (Map<String, Object>) settings.settings().get("rss");
			
			// Getting feeds array
			boolean array = XContentMapValues.isArray(rssSettings.get("feeds"));
			if (array) {
				ArrayList<Map<String, Object>> feeds = (ArrayList<Map<String, Object>>) rssSettings.get("feeds");
				feedsDefinition = new ArrayList<RssRiverFeedDefinition>(feeds.size());
				for (Map<String, Object> feed : feeds) {
					String feedname = XContentMapValues.nodeStringValue(feed.get("name"), null);
					String url = XContentMapValues.nodeStringValue(feed.get("url"), null);
					int updateRate  = XContentMapValues.nodeIntegerValue(feed.get("update_rate"), 15 * 60 * 1000);
                    boolean ignoreTtl = XContentMapValues.nodeBooleanValue(feed.get("ignore_ttl"), false);
					feedsDefinition.add(new RssRiverFeedDefinition(feedname, url, updateRate, ignoreTtl));
				}
				
			} else {
				logger.warn("rss.url and rss.update_rate have been deprecated. Use rss.feeds[].url and rss.feeds[].update_rate instead.");
				logger.warn("See https://github.com/dadoonet/rssriver/issues/6 for more details...");
				String url = XContentMapValues.nodeStringValue(rssSettings.get("url"), null);
				int updateRate  = XContentMapValues.nodeIntegerValue(rssSettings.get("update_rate"), 15 * 60 * 1000);
                boolean ignoreTtl = XContentMapValues.nodeBooleanValue("ignore_ttl", false);

				feedsDefinition = new ArrayList<RssRiverFeedDefinition>(1);
				feedsDefinition.add(new RssRiverFeedDefinition(null, url, updateRate, ignoreTtl));
			}
			
		} else {
			String url = "http://www.lemonde.fr/rss/une.xml";
			logger.warn("You didn't define the rss url. Switching to defaults : [{}]", url);
			int updateRate = 15 * 60 * 1000;
			feedsDefinition = new ArrayList<RssRiverFeedDefinition>(1);
			feedsDefinition.add(new RssRiverFeedDefinition("lemonde", url, updateRate, false));
		}

		
		if (settings.settings().containsKey("index")) {
			Map<String, Object> indexSettings = (Map<String, Object>) settings
					.settings().get("index");
			indexName = XContentMapValues.nodeStringValue(
					indexSettings.get("index"), riverName.name());
			typeName = XContentMapValues.nodeStringValue(
					indexSettings.get("type"), "page");
            bulkSize = XContentMapValues.nodeIntegerValue(
                    indexSettings.get("bulk_size"), 25);
            bulkFlushInterval = TimeValue.parseTimeValue(XContentMapValues.nodeStringValue(
                    indexSettings.get("flush_interval"), "5s"), TimeValue.timeValueSeconds(5));
            maxConcurrentBulk = XContentMapValues.nodeIntegerValue(indexSettings.get("max_concurrent_bulk"), 1);
        } else {
			indexName = riverName.name();
			typeName = "page";
            bulkSize = 100;
            maxConcurrentBulk = 1;
            bulkFlushInterval = TimeValue.timeValueSeconds(5);
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

        try {
            pushMapping(indexName, typeName, RssToJson.buildRssMapping(typeName));
        } catch (Exception e) {
            logger.warn("failed to create mapping for [{}/{}], disabling river...",
                    e, indexName, typeName);
            return;
        }

        // Creating bulk processor
        this.bulkProcessor = BulkProcessor.builder(client, new BulkProcessor.Listener() {
            @Override
            public void beforeBulk(long executionId, BulkRequest request) {
                logger.debug("Going to execute new bulk composed of {} actions", request.numberOfActions());
            }

            @Override
            public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
                logger.debug("Executed bulk composed of {} actions", request.numberOfActions());
                if (response.hasFailures()) {
                    logger.warn("There was failures while executing bulk", response.buildFailureMessage());
                    if (logger.isDebugEnabled()) {
                        for (BulkItemResponse item : response.getItems()) {
                            if (item.isFailed()) {
                                logger.debug("Error for {}/{}/{} for {} operation: {}", item.getIndex(),
                                        item.getType(), item.getId(), item.getOpType(), item.getFailureMessage());
                            }
                        }
                    }
                }
            }

            @Override
            public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
                logger.warn("Error executing bulk", failure);
            }
        })
                .setBulkActions(bulkSize)
                .setConcurrentRequests(maxConcurrentBulk)
                .setFlushInterval(bulkFlushInterval)
                .build();

        // We create as many Threads as there are feeds
		threads = new ArrayList<Thread>(feedsDefinition.size());
		int threadNumber = 0;
		for (RssRiverFeedDefinition feedDefinition : feedsDefinition) {
			Thread thread = EsExecutors.daemonThreadFactory(settings.globalSettings(), "rss_slurper_" + threadNumber)
                    .newThread(new RSSParser(feedDefinition));
			thread.start();
			threads.add(thread);
			threadNumber++;
		}
	}

	@Override
	public void close() {
		if (logger.isInfoEnabled()) logger.info("Closing rss river");
		closed = true;
		
		// We have to close each Thread
		if (threads != null) {
			for (Thread thread : threads) {
				if (thread != null) {
					thread.interrupt();
				}
			}
		}
	}

	
	private SyndFeed getFeed(String url) {
		try {
			URL feedUrl = new URL(url);
			URLConnection openConnection = feedUrl.openConnection();
	        openConnection.addRequestProperty("User-Agent", "RSS River for Elasticsearch (https://github.com/dadoonet/rssriver)"); 
			SyndFeedInput input = new SyndFeedInput();
            input.setPreserveWireFeed(true);
			SyndFeed feed = input.build(new XmlReader(openConnection));
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

    /**
     * Check if a mapping already exists in an index
     * @param index Index name
     * @param type Mapping name
     * @return true if mapping exists
     */
    private boolean isMappingExist(String index, String type) {
        ClusterState cs = client.admin().cluster().prepareState().setIndices(index).execute().actionGet().getState();
        IndexMetaData imd = cs.getMetaData().index(index);

        if (imd == null) return false;

        MappingMetaData mdd = imd.mapping(type);

        if (mdd != null) return true;
        return false;
    }

    private void pushMapping(String index, String type, XContentBuilder xcontent) throws Exception {
        if (logger.isTraceEnabled()) logger.trace("pushMapping("+index+","+type+")");

        // If type does not exist, we create it
        boolean mappingExist = isMappingExist(index, type);
        if (!mappingExist) {
            logger.debug("Mapping ["+index+"]/["+type+"] doesn't exist. Creating it.");

            // Read the mapping json file if exists and use it
            if (xcontent != null) {
                if (logger.isTraceEnabled()) logger.trace("Mapping for ["+index+"]/["+type+"]="+xcontent.string());
                // Create type and mapping
                PutMappingResponse response = client.admin().indices()
                        .preparePutMapping(index)
                        .setType(type)
                        .setSource(xcontent)
                        .execute().actionGet();
                if (!response.isAcknowledged()) {
                    throw new Exception("Could not define mapping for type ["+index+"]/["+type+"].");
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Mapping definition for ["+index+"]/["+type+"] succesfully created.");
                    }
                }
            } else {
                if (logger.isDebugEnabled()) logger.debug("No mapping definition for ["+index+"]/["+type+"]. Ignoring.");
            }
        } else {
            if (logger.isDebugEnabled()) logger.debug("Mapping ["+index+"]/["+type+"] already exists.");
        }
        if (logger.isTraceEnabled()) logger.trace("/pushMapping("+index+","+type+")");
    }

	private class RSSParser implements Runnable {
		private String url;
		private int updateRate;
		private String feedname;
        private boolean ignoreTtl;

        public RSSParser(String feedname, String url, int updateRate, boolean ignoreTtl) {
			this.feedname = feedname;
			this.url = url;
			this.updateRate = updateRate;
            this.ignoreTtl = ignoreTtl;
            if (logger.isInfoEnabled()) logger.info("creating rss stream river [{}] for [{}] every [{}] ms",
                    feedname, url, updateRate);
		}

        public RSSParser(RssRiverFeedDefinition feedDefinition) {
            this(feedDefinition.getFeedname(),
                    feedDefinition.getUrl(),
                    feedDefinition.getUpdateRate(),
                    feedDefinition.isIgnoreTtl());
        }

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
                    if (logger.isDebugEnabled()) logger.debug("Reading feed from {}", url);
                    Date feedDate = feed.getPublishedDate();
                    if (logger.isDebugEnabled()) logger.debug("Feed publish date is {}", feedDate);

                    String lastupdateField = "_lastupdated_" + UUID.nameUUIDFromBytes(url.getBytes()).toString();
                    Date lastDate = getLastDateFromRiver(lastupdateField);
                    // Comparing dates to see if we have something to do or not
                    if (lastDate == null || (feedDate != null && feedDate.after(lastDate))) {
                        // We have to send results to ES
                        if (logger.isTraceEnabled()) logger.trace("Feed is updated : {}", feed);

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
                                if (!oldMessage.isExists()) {
                                    bulkProcessor.add(indexRequest(indexName).type(typeName).id(id).source(toJson(message, riverName.getName(), feedname)));

                                    if (logger.isDebugEnabled()) logger.debug("FeedMessage update detected for source [{}]", feedname != null ? feedname : "undefined");
                                    if (logger.isTraceEnabled()) logger.trace("FeedMessage is : {}", message);
                                } else {
                                    if (logger.isTraceEnabled()) logger.trace("FeedMessage {} already exist. Ignoring", id);
                                }
                            }

                            if (logger.isTraceEnabled()) {
                                logger.trace("processing [_seq  ]: [{}]/[{}]/[{}], last_seq [{}]", indexName, riverName.name(), lastupdateField, feedDate);
                            }
                            // We store the lastupdate date
                            bulkProcessor.add(indexRequest("_river").type(riverName.name()).id(lastupdateField)
                                    .source(jsonBuilder().startObject().startObject("rss").field(lastupdateField, feedDate).endObject().endObject()));
                        } catch (IOException e) {
                            logger.warn("failed to add feed message entry to bulk indexing");
                        }
                    } else {
                        // Nothing new... Just relax !
                        if (logger.isDebugEnabled()) logger.debug("Nothing new in the feed... Relaxing...");
                    }
                }
				try {
                    // #8 : Use the ttl rss field to auto adjust feed refresh rate
                    if (!ignoreTtl && feed.originalWireFeed() != null && feed.originalWireFeed() instanceof Channel) {
                        Channel channel = (Channel) feed.originalWireFeed();
                        if (channel.getTtl() > 0) {
                            int ms = channel.getTtl() * 60 * 1000;
                            if (ms != updateRate) {
                                updateRate = ms;
                                if (logger.isInfoEnabled())
                                    logger.info("Auto adjusting update rate with provided ttl: {} mn", channel.getTtl());
                            }
                        }
                    }

					if (logger.isDebugEnabled()) logger.debug("Rss river is going to sleep for {} ms", updateRate);
					Thread.sleep(updateRate);
				} catch (InterruptedException e1) {
				}
			}
		}

        @SuppressWarnings("unchecked")
		private Date getLastDateFromRiver(String lastupdateField) {
            Date lastDate = null;
            try {
                // Do something
                client.admin().indices().prepareRefresh("_river").execute().actionGet();
                GetResponse lastSeqGetResponse =
                        client.prepareGet("_river", riverName().name(), lastupdateField).execute().actionGet();
                if (lastSeqGetResponse.isExists()) {
                    Map<String, Object> rssState = (Map<String, Object>) lastSeqGetResponse.getSourceAsMap().get("rss");

                    if (rssState != null) {
                        Object lastupdate = rssState.get(lastupdateField);
                        if (lastupdate != null) {
                            String strLastDate = lastupdate.toString();
                            lastDate = ISODateTimeFormat.dateOptionalTimeParser().parseDateTime(strLastDate).toDate();
                        }
                    }
                } else {
                    // First call
                    if (logger.isDebugEnabled()) logger.debug("{} doesn't exist", lastupdateField);
                }
            } catch (Exception e) {
                logger.warn("failed to get _lastupdate, throttling....", e);
            }
            return lastDate;
        }
    }
}
