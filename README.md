RSS River for Elasticsearch
===========================

Welcome to the RSS River Plugin for [Elasticsearch](http://www.elasticsearch.org/)


Versions
--------

* For 1.0.x elasticsearch versions, look at [master branch](https://github.com/dadoonet/rssriver/tree/master).
* For 0.90.x elasticsearch versions, look at [es-0.90 branch](https://github.com/dadoonet/rssriver/tree/es-0.90).

|      RSS River Plugin      |    elasticsearch    | Release date |
|----------------------------|---------------------|:------------:|
| 1.1.0-SNAPSHOT             | 1.0 - master        |  XXXX-XX-XX  |
| 1.0.0                      | 1.0                 |  2014-02-14  |
| 1.0.0.RC1                  | 1.0                 |  2014-02-03  |

Please read documentation relative to the version you are using:

* [1.1.0-SNAPSHOT](https://github.com/dadoonet/rssriver/blob/master/README.md)
* [1.0.0](https://github.com/dadoonet/rssriver/blob/rssriver-1.0.0/README.md)
* [1.0.0.RC1](https://github.com/dadoonet/rssriver/blob/rssriver-1.0.0.RC1/README.md)

Build Status
------------

Thanks to cloudbees for the [build status](https://buildhive.cloudbees.com/job/dadoonet/job/rssriver/) : 
![build status](https://buildhive.cloudbees.com/job/dadoonet/job/rssriver/badge/icon "Build status")


Getting Started
===============

Installation
------------

Just type :

```sh
$ bin/plugin -install fr.pilato.elasticsearch.river/rssriver/1.0.0
```

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
* Update Rate: every 15 minutes (15 * 60 * 1000 = 900000 ms)
* Ignore TTL : true

```sh
$ curl -XPUT 'localhost:9200/_river/lemonde/_meta' -d '{
  "type": "rss",
  "rss": {
    "feeds" : [ {
    	"name": "lemonde",
    	"url": "http://www.lemonde.fr/rss/une.xml",
    	"update_rate": 900000,
    	"ignore_ttl": true
    	}
    ]
  }
}'
```

If you need to get multiple feeds, you can add them :

Feed1

* URL : http://www.lemonde.fr/rss/une.xml
* Update Rate1 : every 15 minutes (15 * 60 * 1000 = 900000 ms) (will be modified by provided TTL)

Feed2

* URL : http://rss.lefigaro.fr/lefigaro/laune
* Update Rate2 : every 30 minutes (30 * 60 * 1000 = 1800000 ms)
* Ignore TTL : true


```sh
$ curl -XPUT 'localhost:9200/actus/' -d '{}'

$ curl -XPUT 'localhost:9200/_river/actus/_meta' -d '{
  "type": "rss",
  "rss": {
    "feeds" : [ {
			"name": "lemonde",
			"url": "http://www.lemonde.fr/rss/une.xml",
			"update_rate": 900000
    	}, {
			"name": "lefigaro",
			"url": "http://rss.lefigaro.fr/lefigaro/laune",
			"update_rate": 1800000,
			"ignore_ttl": true
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
It reads the optional `<pubDate>` tag and store it in Elastic Search to compare it on next launch.

Then, for each `<item>` tag, RSS river creates a new document with the following properties :

|         XML Path         |     ES Mapping    |
|--------------------------|-------------------|
| `/title`                 | title             |
| `/description`           | description       |
| `/author`                | author            |
| `/link`                  | link              |
| `/category`              | category          |
| `/geo:lat` `/geo:long`   | location          |
| `/enclosures[@url]`      | enclosures.url    |
| `/enclosures[@type]`     | enclosures.type   |
| `/enclosures[@length]`   | enclosures.length |

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
