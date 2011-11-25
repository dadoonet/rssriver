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

import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

/**
 * @author dadoonet (David Pilato)
 */
public abstract class AbstractRssRiverTest {

	/**
	 * Define a mapping if needed
	 * @return The mapping to use
	 */
	abstract public XContentBuilder mapping() throws Exception;
	
	/**
	 * Define the Rss River settings
	 * @return Rss River Settings
	 */
	abstract public XContentBuilder rssRiver() throws Exception;
	
	public void launchTest() throws Exception {
		Node node = NodeBuilder
				.nodeBuilder()
				.settings(
						ImmutableSettings.settingsBuilder()
						.put("gateway.type", "local")
						.put("path.data", "./target/es/data")		
						.put("path.logs", "./target/es/logs")		
						.put("path.work", "./target/es/work")		
						).node();
		
		// We wait for one second to let ES start
		Thread.sleep(1000);
		try {
			node.client().admin().indices()
					.delete(new DeleteIndexRequest("_river")).actionGet();
			// We wait for one second to let ES delete the river
			Thread.sleep(1000);
		} catch (IndexMissingException e) {
			// Index does not exist... Fine
		}

		try {
			node.client().admin().indices()
					.delete(new DeleteIndexRequest("rss")).actionGet();
			// We wait for one second to let ES delete the index
			Thread.sleep(1000);
		} catch (IndexMissingException e) {
			// Index does not exist... Fine
		}

		// Creating the index
		node.client().admin().indices().create(new CreateIndexRequest("rss")).actionGet();
		Thread.sleep(1000);

		// If a mapping is defined, we will use it
		if (mapping() != null) {
			node.client().admin().indices()
			.preparePutMapping("rss")
			.setType("page")
			.setSource(mapping())
			.execute().actionGet();
		}

		if (rssRiver() == null) throw new Exception("Subclasses must provide an rss setup...");
		node.client().prepareIndex("_river", "rss", "_meta").setSource(rssRiver())
				.execute().actionGet();

		// Let's wait one hour 
		Thread.sleep(1 * 60 * 60 * 1000);
	}
}
