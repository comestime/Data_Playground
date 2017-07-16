# Project 1. Game Recommendation System


## 1.1 Overview

The purpose of this project is to implement a game recommendation system from scratch. In this project, we are trying to:

* Understand what is recommendation system
* Build a game recommendation system for users
* Save the results to dataware house for future analysis
* Implement a Web UI to display the recommendation results

Based on the objectives, we divide the project into three phases:

1. Data collection using web crawler
2. Game recommendation utilizing Spark
3. Web UI implementation

The following is a diagram of Lambda architecture. We mainly focus on batch layer and serving layer. The speed layer can be implemented by establishing stream data flow for incremental recommender. Note the recommendation algorithm used in this project does support streaming, but this is beyond the scope of this project.

![lambda architecture](recom_proj/lambda-architecture.jpg)


## 1.2 Data Collection Using Web Crawler

Most of the data is extracted from the official Steam Web API. Before you start, register an account in Steam Developer Community and obtain the Steam API key. 

### 1.2.1 Obtain A List of Users

Note that the player summary API has a URL of the form like this:

`
http://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?key=XXXXXXXXXXXXXXXXXXXXXXX&steamids=76561197960435530
`

Thus it's important to obtain a list of users. Obviously, you can traverse all possible user IDs. However, our way to do this is to crawl the Steam website to get a subset of online users. First we import all the necessary packages

```py
# for web crawler
import requests
import json

# used to parse HTML
from bs4 import BeautifulSoup as bs
from contextlib import closing
import urllib
import re

# Steam API key
key = 'xxxxxxxxxxxxxxxxxxxxxxxxxx'
```

By using the following two methods, we can get a list of online users. `get_online_users` will utilize `BeautifulSoup` to parse Steam Community member list page, while `get_user_id` will parse each user's personal profile to get the user ID. All user IDs we crawled will be saved in a list `user_ids`. Note we use `Request` package to get the web content.

```py
# stemID can always be found in PHP script; use urllib to parse response
def get_user_id(user_profile, user_ids):
    url = user_profile
    with closing(urllib.urlopen(url)) as page:
        for line in page:
            if "steamid" in line:
                try: 
                    user_id = re.search("\"steamid\":\"(\d+)\"", line).group(1)
                    print user_id + ' ' + user_profile
                    if user_id != None:
                        user_ids.append(user_id)
                        break
                except:
                    continue

# traverse the member list to find out online/in-game users
def get_online_users(member_list_no, user_ids):
    url = 'https://steamcommunity.com/games/steam/members?p=' + str(member_list_no)
    resp = requests.get(url)

    soup = bs(resp.text, 'html.parser')
    # print(soup.prettify())

    # search profile of users who are online/in-game
    all_users = soup.find_all("div", \
                onclick = re.compile("top\.location\.href='https:\/\/steamcommunity\.com\/id\/(\w+)'"), \
                class_ = re.compile("online|in-game"))

    # get user names
    for user in all_users:
        user_profile = user.div.div.div.a['href'].encode("ascii")
        # print user_profile
        get_user_id(user_profile, user_ids)

# traverse through every member list
# modify the range to get more users
member_list_page_no = 5
user_ids = []
for idx in range(1, member_list_page_no + 1):
    print "Member List " + str(idx)
    get_online_users(idx, user_ids)
print "Total online users found:"
print len(user_ids)
```

You might want to save the member list to an external text file. A sample online member list looks like this:

```
76561197972495328 https://steamcommunity.com/id/FireSlash
76561197960434622 https://steamcommunity.com/id/afarnsworth
76561197968459473 https://steamcommunity.com/id/drunkenf00l
76561197970323416 https://steamcommunity.com/id/tomqbui
76561197963135603 https://steamcommunity.com/id/jigoku
76561197960794555 https://steamcommunity.com/id/killahinstinct_
76561198053398526 https://steamcommunity.com/id/0x6D6178
76561197971155734 https://steamcommunity.com/id/rotNdude
...
```

### 1.2.2 Obtain User Summary from Steam API

The following step is a lot easier with our interested user IDs. The code below will obtain user summary information from Steam API, and dump it into a JSON file.

```py
url = 'http://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?key=' + key + '&steamids='
dump_user_info(url, user_ids, 'user_summary_sample.json')
```

The code under the hood looks like this:

```py
# data cleaning during json object extraction 
# since some fields or json hierarchies are not quite useful
def process_json_obj(resp, user_out_file, user_id):
    if 'user_summary' in user_out_file:
        # corner case: list index out of range
        try:
            obj = resp.json()['response']['players'][0]
        except:
            obj = {'steamid' : user_id}
    elif 'user_owned_games' in user_out_file:
        obj = resp.json()['response']
        obj = {'steamid' : user_id, 'game_count' : obj['game_count'], 'games' : obj['games']}
    elif 'user_friend_list' in user_out_file:
        obj = resp.json()['friendslist']
        obj = {'steamid' : user_id, 'friends' : obj['friends']}
    elif 'user_recently_played_games' in user_out_file:
        obj = resp.json()['response']
        try:
            obj = {'steamid' : user_id, 'total_count' : obj['total_count'], 'games' : obj['games']}
        except:
            # corner case: total_count is zero
            obj = {'steamid' : user_id, 'total_count' : obj['total_count'], 'games' : []}
    return obj

# obtain user information and dump it to JSON file
def dump_user_info(url, user_ids, user_out_file):
    with open(user_out_file, 'w') as f:
        for user_id in user_ids:
            url_temp = url + str(user_id)
            resp = requests.get(url_temp)
            # resp = requests.head(url_temp)
            obj = process_json_obj(resp, user_out_file, user_id)
            json.dump(obj, f)
            f.write('\n')
```

The crawled information is in JSON format, which is preferred. The sample user summary info looks like this:

```json
{
	"steamid": "76561197970565175", 
	"primaryclanid": "103582791429521412",
	"gameid": "24740",
	"realname": "Alden Kroll",
	"personaname": "Alden",
	"personastate": 2,
	"personastateflags": 0,
	"communityvisibilitystate": 3,
	"loccountrycode": "US",
	"profilestate": 1,
	"profileurl":
	"http://steamcommunity.com/id/alden/",
	"loccityid": 4030,
	"timecreated": 1100667337,
	"avatar": "https://steamcdn-a.akamaihd.net/steamcommunity/public/images/avatars/20/20111513204464c620cc8a10e9651760b6587ec3.jpg",
	"locstatecode": "WA",
	"gameextrainfo": "Burnout Paradise: The Ultimate Box",
	"avatarfull": "https://steamcdn-a.akamaihd.net/steamcommunity/public/images/avatars/20/20111513204464c620cc8a10e9651760b6587ec3_full.jpg",
	"avatarmedium": "https://steamcdn-a.akamaihd.net/steamcommunity/public/images/avatars/20/20111513204464c620cc8a10e9651760b6587ec3_medium.jpg",
	"lastlogoff": 1496585916
}
```

### 1.2.3 Obtain Other Information from Steam API

Actually we can reuse the same methods to obtain other information from Steam API, e.g., user owned games, friend list, user recently played games, etc.

The `user owned games` has a schema like this:

```json
{
	"steamid": "76561197970565175",
	"game_count": 782,
	"games": [{"playtime_forever": 162, "appid": 220},
				{"playtime_forever": 104, "appid": 240}, 
				{"playtime_forever": 0, "appid": 280}, 
				{"playtime_forever": 0, "appid": 320},
				...]
}
```

The `friend list` has a schema like this:

```json
{
	"steamid": "76561197970565175", 
	"friends": [
		{"steamid": "76561197960265731", "friend_since": 0, "relationship": "friend"}, 
		{"steamid": "76561197960265733", "friend_since": 0, "relationship": "friend"}, 
		{"steamid": "76561197960265738", "friend_since": 0, "relationship": "friend"}, 
		{"steamid": "76561197960265740", "friend_since": 0, "relationship": "friend"},
		...]
}
```

The `user recently played games` has a schema like this:

```json
{
	"steamid": "76561197970565175", 
	"total_count": 5, 
	"games": [
		{"playtime_forever": 216, 
		 "playtime_2weeks": 130,
		 "name": "Burnout Paradise: The Ultimate Box",
		 "img_logo_url": "196d5d0945d6c608a621cf7d44429644ebeae267",
		 "appid": 24740,
		 "img_icon_url": "eca9b0f2936018e277ee68450cd4fa8d1d3ca3e7"},
		 ...]
}
```

The `game detail` information can also be obtained. This information can be useful in the game recommendation process. The schema looks like this:

```json
{
	"type":"game",
	"name":"Counter-Strike",
	"steam_appid":10,
	"required_age":0,
	"is_free":false,
	"detailed_description":"Play the world's number 1 online action game...",
	...
}
```

### 1.2.4 Web Crawler Improvements

The web crawler we currently use is single threaded. This is just one possible implementation. Different solutions are alwasy welcome. To improve efficiency, there are several things we can do:

* Multithreaded web crawler. The `Scrapy` package can be a strong enhancement.
* If we have a cluster to use, a distributed web crawler can be a powerful tool

The detailed usage is beyond the scope of this project, and readers are encouraged to have a try.


## 1.3 Game Recommendation Utilizing Spark

In this step, we are going to implement data pipeline in Spark and recommendation engine using Spark MLlib library.

### 1.3.1 Data Warehouse

A good practise is to load data into data warehouse before furthur processing. Note usually we need to do some data cleaning as a pre-processing, some factors to consider:

* Data quality
* Data uniquness
* Missing values
* Failed values

That has already been done in Phase 1. Now we load data into Hive, and the suggested schema is shown below

```sql
-- load required SerDe for JSON parsing
ADD JAR json-serde-1.3.9-SNAPSHOT-jar-with-dependencies.jar;ROW FORMAT SERDE 'org.openx.data.jsonserde.JsonSerDe'

-- load data for user summary
CREATE TABLE IF NOT EXISTS steam_user (	steamID STRING,	name STRING,	level STRING,	since STRING,	customURL STRING,	real_name STRING,	location STRING)ROW FORMAT SERDE 'org.apache.hive.hcatalog.data.JsonSerDe'STORED AS TEXTFILE;
LOAD DATA LOCAL INPATH 'user.json' OVERWRITE INTO TABLE steam_user;

-- load data for friend list
CREATE TABLE IF NOT EXISTS friends (	steamID STRING,	friends ARRAY<STRUCT<url:STRING, name:STRING>>)ROW FORMAT SERDE 'org.apache.hive.hcatalog.data.JsonSerDe'STORED AS TEXTFILE;
LOAD DATA LOCAL INPATH 'friends.json' OVERWRITE INTO TABLE friends;

-- load data for game details
CREATE TABLE IF NOT EXISTS game (	id INT,	name STRING,	type STRING,	is_free BOOLEAN,	required_age TINYINT,	detailed_description STRING,	short_description STRING,	about STRING,	supported_language STRING,	header_image STRING,	website STRING,	platforms STRUCT<windows:BOOLEAN,mac:BOOLEAN, linux:BOOLEAN>,	pc_requirements STRUCT< minimum:STRING>,	mac_requirements STRUCT< minimum:STRING>,	linux_requirements STRUCT< minimum:STRING>,	developers ARRAY<STRING>,	publishers ARRAY<STRING>,	price STRUCT<currency:STRING, initial: INT, final: INT, discount_percent: INT>,	categories ARRAY<STRUCT<id:INT, description:STRING> >,	metacritic STRUCT<score:INT,url:STRING>,	genres ARRAY<STRUCT<id:STRING, description:STRING> >,	screenshots ARRAY<STRUCT<id:INT, path_thumbnail:STRING, path_full:STRING>>,	recommendations INT,	achievements INT,	release_date STRUCT<coming_soon:BOOLEAN, release_date:STRING>,	support_info STRUCT<url:STRING, email:STRING>,	background STRING
)ROW FORMAT SERDE 'org.apache.hive.hcatalog.data.JsonSerDe'STORED AS TEXTFILE;
LOAD DATA LOCAL INPATH 'game.json' OVERWRITE INTO TABLE game;```

### 1.3.2 Setting Up Pyspark Environment

To use PySpark in Jupyter Notebook, we need to do some setup. We have already provided a docker container, to simplify the setup. However, you can also use your own way to set up the environment. Follow the steps below

* Install docker in your computer from website `https://docs.docker.com`
* Get docker image specifically for this project, do `docker pull dalverse/all-spark-notebook`
* Prepare a folder to share with docker, e.g., `/Users/admin/working`
* Start docker container, do `docker run -v /Users/admin/working:/home/dal/work -d -P dalverse/all-spark-notebook`
* Check docker running status, do `docker ps -a`; You should be able to get CONTAINER ID, make a note of the port map, e.g., `0.0.0.0:32769->8888/tcp`
* Do `docker logs [CONTAINER ID]`, e.g, `docker logs 3a66b75a85aa`, and look for messages like this `Copy/paste this URL into your browser when you connect for the first time, to login with a token: xxxx`
* Log into Jupyter Notebook with Spark by typing `http://localhost:[host port number]/tree?` and the token

If you follow these steps, you are good to go!

### 1.3.3 Recommendation Algorithms

Recommender systems typically produce a list of recommendations in one of two ways – through collaborative and content-based filtering or the personality-based approach.

**Collaborative filtering** methods are based on collecting and analyzing a large amount of information on users’ behaviors, activities or preferences and predicting what users will like based on their similarity to other users. A key advantage of the collaborative filtering approach is that it does not rely on machine analyzable content and therefore it is capable of accurately recommending complex items such as movies without requiring an "understanding" of the item itself. Collaborative filtering is based on the assumption that people who agreed in the past will agree in the future, and that they will like similar kinds of items as they liked in the past.

![Collaborative Filtering](recom_proj/collaborative_filtering.png)

**Content-based filtering** methods are based on a description of the item and a profile of the user’s preferences. In a content-based recommender system, keywords are used to describe the items and a user profile is built to indicate the type of item this user likes. In other words, these algorithms try to recommend items that are similar to those that a user liked in the past (or is examining in the present). In particular, various candidate items are compared with items previously rated by the user and the best-matching items are recommended. This approach has its roots in information retrieval and information filtering research.

![Content-based Filtering](recom_proj/content_based_filtering.png)

Recent research has demonstrated that a **hybrid approach**, combining collaborative filtering and content-based filtering could be more effective in some cases. Hybrid approaches can be implemented in several ways: by making content-based and collaborative-based predictions separately and then combining them; by adding content-based capabilities to a collaborative-based approach (and vice versa); or by unifying the approaches into one model. Several studies empirically compare the performance of the hybrid with the pure collaborative and content-based methods and demonstrate that the hybrid methods can provide more accurate recommendations than pure approaches. These methods can also be used to overcome some of the common problems in recommender systems such as cold start and the sparsity problem.

![Hybrid Approach](recom_proj/hybrid_approach.jpg)

In this project, we mainly focus on collaborative filtering based recommendation. Readers are encouraged to try content-based filtering, which is typically easier to implement.

### 1.3.4 Alternative Least Square Algorithm

The **collaborive filtering** problem can be formulated as a learning problem in which we are given the ratings that users have given certain items and are tasked with predicting their ratings for the rest of the
items. Formally, if there are n users and m items, we are given an n × m matrix R in which the (u, i)th entry is r_ui – the rating for item i by user u. Matrix R has many missing entries indicating unobserved ratings, and our task is to estimate these unobserved ratings.

A popular approach for this is matrix factorization, where Alternative Least Square (ALS) algorithm renders its power. ALS can not only be implemented in single machine, but also in distributed clusters, or even in streaming. For details, refer to the references.

Before we really start to play around with the algorithm, it's highly recommended to read through the Pyspark collaborative filtering documentations `https://spark.apache.org/docs/latest/ml-collaborative-filtering.html`.

### 1.3.5 Implementation in Pyspark

Before we start, we need to import all required packages

```py
from pyspark import SparkContext
from pyspark.conf import SparkConf
from pyspark.sql import SparkSession, HiveContext, Row
from pyspark.mllib.recommendation import ALS
import json
```

For standalone applications, we need to create SparkContext/HiveContext as the start point

```py
sc = SparkSession \
    .builder \
    .appName("spark-recommender") \
    .getOrCreate()
hiveCtx = HiveContext(sc)
```

We can load the game details into Spark.

```py
df_game = hiveCtx.read.json(game_detail)
df_game.printSchema()
df_game.registerTempTable("temp_game_detail")
# data cleaning: remove corrupted records
df_valid_game = hiveCtx.sql("SELECT * FROM temp_game_detail where _corrupt_record is null")
# register temporary table for future query
df_valid_game.registerTempTable("game_detail")
df_valid_game.show(1)

# root
#  |-- games: array (nullable = true)
#  |    |-- element: struct (containsNull = true)
#  |    |    |-- appid: long (nullable = true)
#  |    |    |-- img_icon_url: string (nullable = true)
#  |    |    |-- img_logo_url: string (nullable = true)
#  |    |    |-- name: string (nullable = true)
#  |    |    |-- playtime_2weeks: long (nullable = true)
#  |    |    |-- playtime_forever: long (nullable = true)
#  |-- steamid: string (nullable = true)
#  |-- total_count: long (nullable = true)
#
# +--------------------+-----------------+-----------+
# |               games|          steamid|total_count|
# +--------------------+-----------------+-----------+
# |[[578080,93d896e7...|76561198097278802|          2|
# +--------------------+-----------------+-----------+
# only showing top 1 row
```

Then we convert the Steam ID to index to avoid overflow in the recommendation algorithm. This is achieved by joining tables.

```py
# df_user_idx = hiveCtx.read.json(sample_user_idx)
df_user_idx = hiveCtx.read.json(full_user_idx)
# df_user_idx.printSchema()
df_user_idx.registerTempTable('user_idx')
df_valid_user_recent_games = hiveCtx.sql("SELECT b.user_idx, a.games FROM user_recent_games a \
                             JOIN user_idx b ON b.user_id = a.steamid WHERE a.total_count != 0")
# df_valid_user_recent_games.printSchema()
df_valid_user_recent_games.show(10)

# +--------+--------------------+
# |user_idx|               games|
# +--------+--------------------+
# |    1459|[[578080,93d896e7...|
# |    1455|[[578080,93d896e7...|
# |       0|[[39210,30461ee9c...|
# |       1|[[501300,f71d2d0c...|
# |    1461|[[377160,779c4356...|
# |    1457|[[377160,779c4356...|
# |    1462|[[570,0bbb630d632...|
# |    1458|[[570,0bbb630d632...|
# |       2|[[578080,93d896e7...|
# |    1463|[[252950,9ad6dd3d...|
# +--------+--------------------+
# only showing top 10 rows
```

Note the input to the training algorithm should be of the format `(user_id, app_id, playtime_forever`

```py
training_rdd = df_valid_user_recent_games.rdd.flatMapValues(lambda x : x) \
                .map(lambda (x, y) : (x, y.appid, y.playtime_forever)) \
                .filter(lambda (x, y, z) : z > 0)
training_rdd.collect()
```

Now we are ready to use the training algorithm

```py
als = ALS.trainImplicit(training_rdd, 10)
result_rating = als.recommendProducts(0,10)
print result_rating

# [Rating(user=0, product=72850, rating=1.080533744968419), Rating(user=0, product=440, rating=0.9969255430731465), ...]
```

and write out the intermediate recommendation results to json files using the method below

```py
with open(full_recommended, 'w') as output_file:
    for user_idx in range(0, df_user_idx.count()):
        try:
            lst_recommended = [i.product for i in als.recommendProducts(user_idx, 10)]
            rank = 1
            for app_id in lst_recommended:
                dict_recommended = {'user_idx': user_idx, 'game_id': app_id, 'rank': rank}
                json.dump(dict_recommended, output_file)
                output_file.write('\n')
                rank += 1
        # some user index may not in the recommendation result since it's been filtered out
        except:
            pass
```

Finally we join the Steam user ID table and game_detail table to form the final results and store the results to external file system

```py
df_recommend_result = hiveCtx.read.json(full_recommended)
# df_recommend_result.show()
df_recommend_result.registerTempTable('recommend_result')
df_final_recommend_result = hiveCtx.sql("SELECT b.user_id, a.rank, c.name, c.header_image, c.steam_appid \
                                        FROM recommend_result a, user_idx b, game_detail c \
                                        WHERE a.user_idx = b.user_idx AND a.game_id = c.steam_appid \
                                        ORDER BY b.user_id, a.rank")
df_final_recommend_result.show()
df_final_recommend_result.write.save(full_final_recommended, format="json")
```

The recommendation results look like this

```
+-----------------+----+--------------------+--------------------+-----------+
|          user_id|rank|                name|        header_image|steam_appid|
+-----------------+----+--------------------+--------------------+-----------+
|76561197960268841|   1|     Team Fortress 2|http://cdn.akamai...|        440|
|76561197960268841|   2|         Garry's Mod|http://cdn.akamai...|       4000|
|76561197960268841|   3|            Terraria|http://cdn.akamai...|     105600|
|76561197960268841|   4|The Elder Scrolls...|http://cdn.akamai...|      72850|
|76561197960268841|   5|              Arma 3|http://cdn.akamai...|     107410|
|76561197960268841|   6|      Clicker Heroes|http://cdn.akamai...|     363970|
|76561197960268841|   6|      Clicker Heroes|http://cdn.akamai...|     363970|
|76561197960268841|   7|                Rust|http://cdn.akamai...|     252490|
|76561197960268841|   8|           Starbound|http://cdn.akamai...|     211820|
|76561197960268841|   9|      Rocket League®|http://cdn.akamai...|     252950|
|76561197960268841|  10|    Source Filmmaker|http://cdn.akamai...|       1840|
|76561197960269425|   1|              Dota 2|http://cdn.akamai...|        570|
|76561197960269425|   2|      Counter-Strike|http://cdn.akamai...|         10|
|76561197960269425|   3|Counter-Strike: G...|http://cdn.akamai...|        730|
|76561197960269425|   4|PLAYERUNKNOWN'S B...|http://cdn.akamai...|     578080|
|76561197960269425|   5|      Rocket League®|http://cdn.akamai...|     252950|
|76561197960269425|   6|The Elder Scrolls...|http://cdn.akamai...|      72850|
|76561197960269425|   7|       Left 4 Dead 2|http://cdn.akamai...|        550|
|76561197960269425|   8|       7 Days to Die|http://cdn.akamai...|     251570|
|76561197960269425|   9|       Path of Exile|http://cdn.akamai...|     238960|
+-----------------+----+--------------------+--------------------+-----------+
only showing top 20 rows
```

You can also try to store the recommendation results to somewhere else, for example, AWS S3 or AWS RDS, for furthur processing. This is beyond the scope of the project, and the readers are encouraged to try it out.


## 1.4 Web UI Implementation

In this phase, we are going to implement a Web UI to present the recommendation results. The Web framework we are using is called Flaskr, which provides a simple interface for dynamically generating responses to web requests.

Before you start, be sure to read through the Flaskr tutorial in here `http://flask.pocoo.org/docs/0.12/tutorial/`. That can help you gain more understanding in what Flaskr is and how Flaskr is organized.

### 1.4.1 An Example Flaskr Implementation

An example Flaskr implementation is shown below.

```py
# import all necessary packages
from flask import Flask, render_template
from flask_sqlalchemy import SQLAlchemy
import re

# instantiate a Flask object, and do some configuation
# i.e., providing database location, etc.
app = Flask(__name__)
DB_URI = 'xxxxxxxx'
app.config['SQLALCHEMY_DATABASE_URI'] = DB_URI
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False

# instantiate a database object
db = SQLAlchemy(app)

# a DB model for table you want to query
class recommendation(db.Model):
    __tablename__ = 'final_recommend'
    user_id = db.Column('user_id', db.Text, primary_key = True)
    rank = db.Column('rank', db.Integer, primary_key = True)
    name = db.Column('name', db.Text)
    header_image = db.Column('header_image', db.Text)
    steam_appid = db.Column('steam_appid', db.Integer)

    def __init__(self, user_id, rank, name, header_image, steam_appid):
        self.user_id = user_id
        self.rank = rank
        self.name = name
        self.header_image = header_image
        self.steam_appid = steam_appid

# this is how you want to route the <user_id> page
# and what parameters you pass to <user_id> template
@app.route('/<user_id>')
def user_recommendation(user_id):
    user_recom = recommendation.query.filter_by(user_id=user_id).order_by(recommendation.rank).all()
    return render_template("user.html", user_recom = user_recom)

if __name__ == '__main__':
    app.run()
```

The corresponding HTML template is shown below

```html
<!DOCTYPE html>
<html class="no-js">
<head>
    <!-- Meta info -->
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Recommended Games</title>
    <meta content="" name="description">
    <meta name="author" content="">
    <meta name="format-detection" content="">

    <!-- Styles -->
    <link href='http://fonts.googleapis.com/css?family=Source+Sans+Pro:300,400,600' rel='stylesheet' type='text/css'>
    <link href="{{ url_for('static', filename="main.css") }}" rel="stylesheet" media="screen, print" type="text/css">
    <script src="{{ url_for('static', filename="modernizr-2.6.2.js") }}" type="text/javascript"></script>
</head>
<body>
    <div id="container" style="left: 0px;">
        <section id="content">
            
<header>
    <div id="pfd">
        <a href="index.html">
            <img src="{{ url_for('static', filename="header_logo.ico") }}" style="width:120px;height:120px;", alt="Comestime Website">
        </a>
    </div>
    <div id="preamble" class="home">
        <div class="preamble">
	<h1>
		Recommended games for Steam user ({{ user_recom[0].user_id }}) using collaborative-filtering algorithm. Sounds exciting, huh? 
		We get each user 10 recommendations, and users can explore more by clicking the game header image</h1>
	<p class="em" style="display: none;">
		Contact me by <a href="mailto:comestime@gmail.com">writing me an email</a></p>
	<a class="read-more" href="http://stanford.edu/~rezab/classes/cme323/S15/notes/lec14.pdf">Read more +</a></div>
<p>&nbsp;
	</p>

    </div>
</header>
<div id="our-work">
    <ul>
        {% for item in user_recom %}
            <li>
                <a href="http://steamcommunity.com/app/{{ item.steam_appid }}">
                    <img src={{ item.header_image }} style="width:450px;height:360px;" alt="{{ item.name }}">
                    <div class="overlay">
                    <summary>
                        <h2></h2>
                        <h3></h3>
                    </summary>
                    <div class="loves">
                    <span>65</span>
                    </div></div>
                </a>
            </li>
        {% endfor %}
    </ul>
</div>
</section>

       <footer id="msf">
            <div class="wrapper">
                <ul id="lets-be-social">
                    <li>
                        <a href="https://www.facebook.com/tengalickwang" rel="external" target="_blank" title="Be comestime's Facebook friend">Facebook.</a>
                    </li>
                    <li>
                        <a href="http://www.linkedin.com/in/comestime" rel="external" target="_blank" title="My LinkedIn Profile">LinkedIn.</a>
                    </li>
                </ul>
                
            </div><!-- wrapper -->
        </footer><!-- footer -->
    </div><!-- container -->
        
    <nav id="toc">
        <ul>
            <li>
                <a class="active" href="index.html">Work.</a></li>
            <li>
                <a href="about.html">About.</a></li>
            <li>
                <a href="what-we-do.html">What we do.</a></li>
        </ul>
	</nav>      
       
    <div id="no-script">
        <div>
            <p>
                The Pyaari website is fully responsive and requires Javascript.<br>
                Please <a href="http://enable-javascript.com/">enable javascript</a> to use this site without issue.</p>
        </div>
    </div><!-- no-script -->
        
    <script src="{{ url_for('static', filename="jquery-1.11.0.min.js") }}"></script>
    <script src="{{ url_for('static', filename="pyaari-main.1.0.js") }}"></script>
    <script src="{{ url_for('static', filename="scripts/pyaari-menu.1.0.js") }}"></script>
    <script>
        $(document).ready(function () {
            PfdMenu._ctor();
        });
    </script>
    

    <script>
    	$(document).ready(function(){
	       ReadMore.init();
    	})
    </script>
</body>
```

Test your Flaskr app in localhost mode by running the Python application, like this

```bash
python flaskapp.py
```

You should be able to see your website brought up in the browser. Note this is just one example of how we can implement this Web UI using Flaskr. Don't be restricted by this example.

### 1.4.2 AWS Introduction

Amazon Web Services (AWS) provides on-demand computing resources and services in the cloud, with pay-as-you-go pricing. For example, you can run a server on AWS that you can log on to, configure, secure, and run just as you would a server that's sitting in front of you.

Using AWS resources instead of your own is like purchasing electricity from a power company instead of running your own generator, and it provides many of the same benefits: capacity exactly matches your need, you pay only for what you use, economies of scale result in lower costs, and the service is provided by a vendor experienced in running large-scale networks.

You can use AWS to make it easier to build and manage your websites and applications. The following are some common uses for AWS:

* Store public or private data.
* Host a static website. These websites use client-side technologies (such as HTML, CSS, and JavaScript) to display content that doesn't change frequently. A static website doesn't require server-side technologies (such as PHP and ASP.NET).
* Host a dynamic website, or web app. These websites include classic three-tier applications, with web, application, and database tiers.
* Support students or online training programs.
* Process business and scientific data.
* Handle peak loads.

In this project, we are using EC2 instance to host our Web UI. The procedure to setup an AWS EC2 instance is shown here 

`http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/EC2_GetStarted.html#ec2-launch-instance`

You should be able to successfully create the EC2 instance by following the tutorial.

Remember to configure the security group as shown below. This setting allows access to port 80 (HTTP) from anywhere, and ssh access only from your IP address.

![aws_security_group](recom_proj/aws_security_group.png)

### 1.4.3 Running the Flaskr App on AWS EC2

SSH to your EC2 instance, and install the apache webserver and mod_wsgi

```bash
$ sudo apt-get update
$ sudo apt-get install apache2
$ sudo apt-get install libapache2-mod-wsgi
```

If you point your browser at your instance's public DNS name, you should see some version of the apache server's assuring "It works!" page.

![apache_server_welcome_page](recom_proj/apache_server_welcome_page.png)

Next install pip tool, and Flask package

```bash
$ sudo apt-get install python-pip
$ sudo pip install flask
```

Then we need to create a directory for our Flask app. We'll create a directory in our home directory to work in, and link to it from the site-root defined in apache's configuration (`/var/www/html` by defualt, see `/etc/apache2/sites-enabled/000-default.conf` for the current value).

```bash
$ mkdir ~/flaskapp
$ sudo ln -sT ~/flaskapp /var/www/html/flaskapp
```

To verify our operation is working, create a simple `index.html` file.

```bash
$ cd ~/flaskapp
$ echo "Hello World" > index.html
```

You should now see "Hello World" displayed if you navigate to `(your instance public DNS)/flaskapp` in your browser.

![flask_hello_world](recom_proj/flask_hello_world.png)

Our server is now running and ready to crunch some data (if something isn't working, try checking the log file in `/var/log/apache2/error.log`).

Now we are ready to deploy the Flaskr app from previous section. Create a `flaskapp.wsgi` file to load the app

```py
import sys
sys.path.insert(0, '/var/www/html/flaskapp')

from flaskapp import app as application
```

The apache server displays html pages by default but to serve dynamic content from a Flask app we'll have to make a few changes. In the apache configuration file located at `/etc/apache2/sites-enabled/000-default.conf`, add the following block just after the DocumentRoot `/var/www/html` line:

```
WSGIDaemonProcess flaskapp threads=5
WSGIScriptAlias / /var/www/html/flaskapp/flaskapp.wsgi

<Directory flaskapp>
    WSGIProcessGroup flaskapp
    WSGIApplicationGroup %{GLOBAL}
    Order deny,allow
    Allow from all
</Directory>
```

![enable_wsgi](recom_proj/enable_wsgi.png)

Now use this command below to restart the server with the new configuration

```bash
$ sudo apachectl restart
```

If everything works correctly, this is the UI you would see from browser

![UI](recom_proj/UI.png)


## 1.5 References

1. Steam Web API - `https://developer.valvesoftware.com/wiki/Steam_Web_API`
2. Request Package - `http://docs.python-requests.org/en/master/`
3. Scrapy Package - `https://scrapy.org/`
4. Python Distributed Web Crawler - `https://github.com/Diastro/Zeek`
5. Docker - `https://docs.docker.com/`
6. Recommendation System - `https://en.wikipedia.org/wiki/Recommender_system`
7. Alternative Least Square - `http://stanford.edu/~rezab/classes/cme323/S15/notes/lec14.pdf`
8. Streaming Matrix Factorization - `https://github.com/brkyvz/streaming-matrix-factorization`
9. Spark MLlib Collaborative Filtering - `https://spark.apache.org/docs/latest/ml-collaborative-filtering.html`
10. Flaskr Tutorial - `http://flask.pocoo.org/docs/0.12/tutorial/`
11. AWS EC2 Introduction - `http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/concepts.html`
12. Getting Started with AWS - `http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/EC2_GetStarted.html#ec2-launch-instance`