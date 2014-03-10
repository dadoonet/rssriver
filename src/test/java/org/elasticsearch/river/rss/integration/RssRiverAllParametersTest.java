/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

package org.elasticsearch.river.rss.integration;

import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.common.base.Predicate;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.hamcrest.Matchers.equalTo;

/**
 * Test all river settings
 */
@ElasticsearchIntegrationTest.ClusterScope(scope = ElasticsearchIntegrationTest.Scope.SUITE, numNodes = 1)
public class RssRiverAllParametersTest extends ElasticsearchIntegrationTest {

    private XContentBuilder startRiverDefinition() throws IOException {
        return jsonBuilder().startObject()
                .field("type", "rss")
                .startObject("rss")
                    .startArray("feeds");
    }

    private XContentBuilder endRiverDefinition(XContentBuilder xcb) throws IOException {
        return xcb.endArray().endObject()
                .startObject("index")
                .field("flush_interval", "500ms")
                .endObject()
                .endObject();
    }

    private void startRiver(final String riverName, final String lastupdate_id, XContentBuilder river) throws InterruptedException {
        logger.info("  --> starting river [{}]", riverName);
        createIndex(riverName);
        index("_river", riverName, "_meta", river);

        // We wait for some documents been processed
        waitForChange(riverName, lastupdate_id);

        // Make sure we refresh indexed docs before launching tests
        refresh();
    }

    private void waitForChange(final String riverName, final String lastupdate_id) throws InterruptedException {
        String date = "";
        GetResponse getResponse = client().prepareGet("_river", riverName, lastupdate_id)
                .setFields("rss." + lastupdate_id)
                .execute().actionGet();
        if (getResponse.isExists()) {
            date = getResponse.getField("rss." + lastupdate_id).getValue().toString();
        }

        final String finalDate = date;
        assertThat("Date should have changed " + date, awaitBusy(new Predicate<Object>() {
            @Override
            public boolean apply(Object o) {
                GetResponse getResponse = client().prepareGet("_river", riverName, lastupdate_id)
                        .setFields("rss." + lastupdate_id)
                        .execute().actionGet();
                String new_date = "";
                if (getResponse.isExists() && getResponse.getField("rss." + lastupdate_id) != null) {
                    new_date = getResponse.getField("rss." + lastupdate_id).getValue().toString();
                }
                return !finalDate.equals(new_date);
            }
        }, 10, TimeUnit.SECONDS), equalTo(true));
    }

    @After
    public void tearDown() throws Exception {
        logger.info("  --> stopping rivers");
        // We need to make sure that the _river is stopped
        wipeIndices("_river");

        // We have to wait a little because it could throw java.lang.RuntimeException
        Thread.sleep(1000);
        super.tearDown();
    }

    private File URItoFile(URL url) {
        try {
            return new File(url.toURI());
        } catch(URISyntaxException e) {
            return new File(url.getPath());
        }
    }

    private String getUrl(String dir) throws IOException {
        URL resource = RssRiverAllParametersTest.class.getResource("/elasticsearch.yml");
        File parent = URItoFile(resource).getParentFile();
        String filename = parent.getCanonicalPath() + File.separator + dir;
        File dataDir = new File(filename);
        if (!dataDir.exists()) {
            logger.error("directory [src/test/resources/{}] should be copied to [{}]", dir, filename);
            throw new RuntimeException("src/test/resources/" + dir + " doesn't seem to exist. Check your JUnit tests.");
        }

        return dataDir.toURI().toString();
    }

    /**
     * Add a river definition
     * @param xcb current xcontent builder
     * @param name feed name (optional)
     * @return _lastupdated_ document id
     */
    private String addLocalRiver(XContentBuilder xcb, String name) throws IOException {
        return addRiver(xcb, getUrl(name + File.separator + "rss.xml"), name);
    }

    /**
     * Add a river definition
     * @param xcb current xcontent builder
     * @param url URL to add
     * @param name feed name (optional)
     * @return _lastupdated_ document id
     */
    private String addRiver(XContentBuilder xcb, String url, String name) {
        try {
            xcb.startObject()
                    .field("url", url)
                    .field("update_rate", 10000);
            if (name != null) {
                xcb.field("name", name);
            }
            xcb.endObject();
        } catch (Exception e) {
            logger.error("fail to add river feed url [{}]", url);
            fail("fail to add river feed");
        }

        return "_lastupdated_" + UUID.nameUUIDFromBytes(url.getBytes()).toString();
    }

    private void existSomeDocs(final String index) throws InterruptedException {
        existSomeDocs(index, null);
    }

    private void existSomeDocs(final String index, final String source) throws InterruptedException {
        // We wait up to 5 seconds before considering a failing test
        assertThat("Some documents should exist...", awaitBusy(new Predicate<Object>() {
            @Override
            public boolean apply(Object o) {
                QueryBuilder query;
                if (source == null) {
                    query = QueryBuilders.matchAllQuery();
                } else {
                    query = QueryBuilders.queryString(source).defaultField("feedname");
                }
                CountResponse response = client().prepareCount(index)
                        .setQuery(query).execute().actionGet();
                return response.getCount() > 0;
            }
        }, 10, TimeUnit.SECONDS), equalTo(true));
    }

    /**
     * Index http://www.lemonde.fr/rss/une.xml sample
     */
    @Test
    public void test_simple_river() throws IOException, InterruptedException {
        XContentBuilder river = startRiverDefinition();
        String lastupdate_id = addLocalRiver(river, "lemonde");
        startRiver("simple", lastupdate_id, endRiverDefinition(river));

        // We wait for some documents
        existSomeDocs("simple");
    }

    /**
     * Index http://www.lemonde.fr/rss/une.xml sample
     * Index http://rss.lefigaro.fr/lefigaro/laune sample
     */
    @Test
    public void test_multiple_river() throws IOException, InterruptedException {
        XContentBuilder river = startRiverDefinition();
        String lastupdate_id = addLocalRiver(river, "lemonde");
        addLocalRiver(river, "lefigaro");
        startRiver("multiple", lastupdate_id, endRiverDefinition(river));

        // We wait for some documents
        existSomeDocs("multiple", "lemonde");
        existSomeDocs("multiple", "lefigaro");
    }

    /**
     * http://www.malwaredomains.com/wordpress/?feed=rss
     * http://www.darkreading.com/rss/all.xml
     */
    @Test
    public void test_mcapp_rivers() throws IOException, InterruptedException {
        XContentBuilder river = startRiverDefinition();
        String lastupdate_id = addLocalRiver(river, "malwaredomains");
        addLocalRiver(river, "darkreading");
        startRiver("mcapp", lastupdate_id, endRiverDefinition(river));

        // We wait for some documents
        existSomeDocs("mcapp", "malwaredomains");
        existSomeDocs("mcapp", "darkreading");
    }
}
