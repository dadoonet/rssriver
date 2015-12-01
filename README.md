RSS River for Elasticsearch (PROJECT STOPPED)
===========================

Welcome to the RSS River Plugin for [Elasticsearch](http://www.elasticsearch.org/)

In order to install the plugin, run: 

```sh
bin/plugin -install fr.pilato.elasticsearch.river/rssriver/1.3.0
```

You need to install a version matching your Elasticsearch version:

|       Elasticsearch    |  RSS River Plugin |                                                            Docs                                                              |
|------------------------|-------------------|------------------------------------------------------------------------------------------------------------------------------|
|    master              | Build from source | See below                                                                                                                    |
|    es-1.x              | Build from source | [1.5.0-SNAPSHOT](https://github.com/dadoonet/rssriver/tree/es-1.x/#version-150-snapshot-for-elasticsearch-1x)                |
|    es-1.4              | Build from source | [1.4.0-SNAPSHOT](https://github.com/dadoonet/rssriver/tree/es-1.x/#version-140-snapshot-for-elasticsearch-14)                |
|    es-1.3              |     1.3.0         | [1.3.0](https://github.com/dadoonet/rssriver/tree/v1.3.0/#version-130-for-elasticsearch-13)                  |
|    es-1.1              |     1.1.0         | [1.1.0](https://github.com/dadoonet/rssriver/tree/v1.2.0/#rss-river-for-elasticsearch)                                       |
|    es-1.0              |     1.0.0         | [1.0.0](https://github.com/dadoonet/rssriver/tree/v1.0.0/#rss-river-for-elasticsearch)                                       |
|    es-0.90             |     0.3.0         | [0.3.0](https://github.com/dadoonet/rssriver/tree/v0.5.0/#rss-river-for-elasticsearch)                                       |

To build a `SNAPSHOT` version, you need to build it with Maven:

```bash
mvn clean install
plugin --install rssriver \ 
       --url file:target/releases/rssriver-X.X.X-SNAPSHOT.zip
```

Build Status
------------

Thanks to cloudbees for the [build status](https://buildhive.cloudbees.com/job/dadoonet/job/rssriver/) : 
![build status](https://buildhive.cloudbees.com/job/dadoonet/job/rssriver/badge/icon "Build status")


Getting Started
===============

Creating a RSS river
--------------------

We create first an index to store all the *feed documents* :

```sh 
$ curl -XPUT 'localhost:9200/lemonde/' -d '{}'
```

We create the river with the following properties :

* Feed URL : http://www.lemonde.fr/rss/une.xml

```sh
$ curl -XPUT 'localhost:9200/_river/lemonde/_meta' -d '{
  "type": "rss",
  "rss": {
    "feeds" : [ {
    	"name": "lemonde",
    	"url": "http://www.lemonde.fr/rss/une.xml"
    	}
    ]
  }
}'
```

This RSS feed follows RSS 2.0 specifications and provide a
[ttl entry](http://www.rssboard.org/rss-specification#ltttlgtSubelementOfLtchannelgt).
The update rate will be auto-adjusted following this value.

If you want to set your own refresh rate (if not provided) and force it (even if it's provided), use
`update_rate` and `ignore_ttl` options:

We create the river with the following properties :

* Feed URL: http://www.lemonde.fr/rss/une.xml
* Update Rate: every 15 minutes
* Ignore TTL : true

```sh
$ curl -XPUT 'localhost:9200/_river/lemonde/_meta' -d '{
  "type": "rss",
  "rss": {
    "feeds" : [ {
    	"name": "lemonde",
    	"url": "http://www.lemonde.fr/rss/une.xml",
    	"update_rate": "15m",
    	"ignore_ttl": true
    	}
    ]
  }
}'
```

If you need to get multiple feeds, you can add them :

Feed1

* URL : http://www.lemonde.fr/rss/une.xml
* Update Rate1 : every 15 minutes (will be modified by provided TTL)

Feed2

* URL : http://rss.lefigaro.fr/lefigaro/laune
* Update Rate2 : every 30 minutes
* Ignore TTL : true


```sh
$ curl -XPUT 'localhost:9200/actus/' -d '{}'

$ curl -XPUT 'localhost:9200/_river/actus/_meta' -d '{
  "type": "rss",
  "rss": {
    "feeds" : [ {
			"name": "lemonde",
			"url": "http://www.lemonde.fr/rss/une.xml",
			"update_rate": "15m"
    	}, {
			"name": "lefigaro",
			"url": "http://rss.lefigaro.fr/lefigaro/laune",
			"update_rate": "30m",
			"ignore_ttl": true
    	}
    ]
  }
}'
```

Indexing raw encoded content
----------------------------

By default, any encoded content provided in `content:encoded` will be indexed under `raw.TYPE` field, where `TYPE`
depends on encoded content type, for example `html`.

You can disable this if you want to save some disk space for example, using `raw` setting:


```sh
$ curl -XPUT 'localhost:9200/_river/actus/_meta' -d '{
  "type": "rss",
  "rss": {
    "raw" : false,
    "feeds" : [ {
			"url": "http://www.lemonde.fr/rss/une.xml"
    	}
    ]
  }
}'
```

Working with mappings
---------------------

If you don't define an explicit mapping before starting RSS river, one will be created by default:

```javascript
{
  "page" : {
    "properties" : {
      "feedname" : {
        "type" : "string",
        "index" : "not_analyzed"
      },
      "title" : {
        "type" : "string"
      },
      "author" : {
        "type" : "string"
      },
      "description" : {
        "type" : "string"
      },
      "link" : {
        "type" : "string",
        "index" : "no"
      },
      "source" : {
        "type" : "string"
      },
      "publishedDate" : {
        "type" : "date",
        "format" : "dateOptionalTime",
        "store" : "yes"
      },
      "location" : {
        "type" : "geo_point"
      },
      "categories" : {
        "type" : "string",
        "index" : "not_analyzed"
      },
      "enclosures" : {
        "properties" : {
          "url" : {
            "type" : "string",
            "index" : "no"
          },
          "type" : {
            "type" : "string",
            "index" : "not_analyzed"
          },
          "length" : {
            "type" : "long",
            "index" : "no"
          }
        }
      },
      "medias" : {
        "properties" : {
          "type" : {
            "type" : "string",
            "index" : "not_analyzed"
          },
          "reference" : {
            "type" : "string",
            "index" : "no"
          },
          "language" : {
            "type" : "string",
            "index" : "not_analyzed"
          },
          "title" : {
            "type" : "string"
          },
          "description" : {
            "type" : "string"
          },
          "duration" : {
            "type" : "long",
            "index" : "no"
          },
          "width" : {
            "type" : "long",
            "index" : "no"
          },
          "height" : {
            "type" : "long",
            "index" : "no"
          }
        }
      },
      "raw" : {
        "properties" : {
          "html" : {
            "type" : "string",
            "index" : "no"
          }
        }
      }
    }
  }
}
```

If you want to define your own mapping, push it to elasticsearch before RSS river starts:

```sh
$ curl -XPUT 'http://localhost:9200/lefigaro/' -d '{}'

$ curl -XPUT 'http://localhost:9200/lefigaro/page/_mapping' -d '{
    "page" : {
        "properties" : {
            "feedname" : {"type" : "string"},
            "title" : {"type" : "string", "analyzer" : "french"},
            "description" : {"type" : "string", "analyzer" : "french"},
            "author" : {"type" : "string"},
            "link" : {"type" : "string"}
        }
    }
}'
```

Then, your feed will use it when you create the river :

```sh
$ curl -XPUT 'localhost:9200/_river/lefigaro/_meta' -d '{
  "type": "rss",
  "rss": {
    "feeds" : [ {
		    "url": "http://rss.lefigaro.fr/lefigaro/laune"
	    }
    ]
  }
}'
```

Bulk settings
-------------

By default, documents are indexed every `25` *feed documents* or every `5` seconds in `river name` index under a `page` type.
You can change those settings when creating the river:

```sh
$ curl -XPUT 'localhost:9200/_river/lemonde/_meta' -d '{
  "type": "rss",
  "rss": {
    "feeds" : [ {
    	"name": "lemonde",
    	"url": "http://www.lemonde.fr/rss/une.xml"
    	}
    ]
  },
  "index": {
    "index": "myindexname",
    "type": "mycontent",
    "bulk_size": 100,
    "flush_interval": "30s"
  }
}'
```

Behind the scene
================

RSS river downloads RSS feed every `update_rate` milliseconds and check if there is new messages.

At first, RSS river look at the `<channel>` tag.
It reads the optional `<pubDate>` tag and store it in Elasticsearch to compare it on next launch.

Then, for each `<item>` tag, RSS river creates a new document with the following properties:

|         XML Path           |     ES Mapping    |
|----------------------------|-------------------|
| `/title`                   | title             |
| `/description`             | description       |
| `/content:encoded`         | raw.html          |
| `/author`                  | author            |
| `/link`                    | link              |
| `/category`                | category          |
| `/geo:lat` `/geo:long`     | location          |
| `/enclosures[@url]`        | enclosures.url    |
| `/enclosures[@type]`       | enclosures.type   |
| `/enclosures[@length]`     | enclosures.length |
| `/media:content[@height]`  | medias.height     |
| `/media:content[@width]`   | medias.width      |
| `/media:content[@url]`     | medias.reference  |
| `/media:content[@type]`    | medias.type       |
| `/media:content[@duration]`| medias.duration   |
| `/media:content[@lang]`    | medias.language   |
| `/media:description`       | medias.description|
| `/media:title`             | medias.title      |

`<content:encoded>` tag will be stored in `raw` object. If `html` content, it will be stored as `raw.html`.

`ID` is generated from description using the [UUID](http://docs.oracle.com/javase/7/docs/api/java/util/UUID.html) generator. So, each message is indexed only once.

Read [RSS 2.0 Specification](http://www.rssboard.org/rss-specification) for more details about RSS channels.

To Do List
==========

Many many things to do :

* As `<pubDate>` tag is optional, we have to check if RSS River is working in that case and parse each feed message
* Support more RSS `<channel>` sub-elements, such as `<category>`, `<skipDays>`, `<skipHours>`
* Support more RSS `<item>` sub-elements, such as `<pubDate>`
* Support for multi-channel (one per language for instance)
* Use `<guid>` as the text to encode to generate `ID`

License
=======

```
This software is licensed under the Apache 2 license, quoted below.

Copyright 2011-2014 David Pilato

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.
```
