## README


### Steps to run the program:

1. Prerequisites:
* spark-2.1.1-bin-hadoop2.7 (or using `conda install -c conda-forge pyspark=2.1.1` to install pyspark packages for Anaconda)
* kafka_2.11-0.10.2.0
* kafka-python (https://kafka-python.readthedocs.io/en/master/install.html) needs to be imported in "source.py"
 
2. Start Zookeeper/Kafka-server in local mode; skip this step in cluster mode
```bash
$ bin/zookeeper-server-start.sh config/zookeeper.properties
$ bin/kafka-server-start.sh config/server.properties
```
 
3. Create topic:
```bash
# local mode
$ kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic fintech_pyspark
$ kafka-topics.sh --list --zookeeper localhost:2181
```
or 

```
# cluster mode
$ /usr/hdp/current/kafka-broker/bin/kafka-topics.sh --create --zookeeper m1.mt.dataapplab.com:2181 --replication-factor 1 --partitions 1 --topic fintech_pyspark
$ /usr/hdp/current/kafka-broker/bin/kafka-topics.sh --list --zookeeper m1.mt.dataapplab.com:2181
```
 
4. Start Kafka source
```bash
# local mode
$ python source.py local
```

or 

```bash
# cluster mode
$ python source.py cluster
```
 
5. Submit Spark job
```bash
# local mode
$ spark-submit --packages org.apache.spark:spark-streaming-kafka-0-8_2.11:2.0.1 streaming.py local fintech_pyspark
```

or

``````bash
# if multiple Spark version exists, need to specify SPARK_MAJOR_VERSION
$ export SPARK_MAJOR_VERSION = 2

# cluster mode
$ spark-submit --packages org.apache.spark:spark-streaming-kafka-0-8_2.11:2.0.1 --master yarn --deploy-mode cluster streaming.py cluster fintech_pyspark
``````

### Snapshot of the Application in Local Mode

![Snapshot 1](image/snapshot_1.png)

![Snapshot 2](image/snapshot_2.png)


### Known Problem and Future Improvements

1. Records are inserted to Hive in the granularity of RDD, generating too many small files in Hive internal table's metastore; HBase may be a better candidate of data warehouse in streaming application

2. Cluster deployment still needs tuning


### References

1. Design Patterns for using foreachRDD (one of the output operations on DStream)
`https://spark.apache.org/docs/latest/streaming-programming-guide.html#output-operations-on-dstreams`

2. Example of using Spark SQL with DStream
`https://github.com/apache/spark/blob/v2.2.0/examples/src/main/python/streaming/sql_network_wordcount.py`

3. ETL Using Spark Streaming and Hive (Scala)
`https://vivekmangipudi.wordpress.com/2017/02/13/hive-etl-spark-streaming-with-hive/`

4. Running Spark on YARN with Hadoop Cluster
`https://nofluffjuststuff.com/blog/mark_johnson/2016/02/5_steps_to_get_started_running_spark_on_yarn_with_a_hadoop_cluster`