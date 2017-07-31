## README


### Steps to run the program:

1. Prerequisites:
* spark-2.1.1-bin-hadoop2.7 (or using `conda install -c conda-forge pyspark=2.1.1` to install pyspark packages for Anaconda)
* kafka_2.11-0.10.2.0
* kafka-python (https://kafka-python.readthedocs.io/en/master/install.html) needs to be imported in "source.py"
 
2. Start Zookeeper/Kafka-server
```bash
$ bin/zookeeper-server-start.sh config/zookeeper.properties
$ bin/kafka-server-start.sh config/server.properties
```
 
3. Create topic:
```bash
$ bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic fintech
$ bin/kafka-topics.sh --list --zookeeper localhost:2181
```
 
4. Start Kafka source
```bash
$ python source.py
```
 
5. Submit Spark job
```bash
$ spark-submit --packages org.apache.spark:spark-streaming-kafka-0-8_2.11:2.0.1 streaming.py localhost:2181 fintech
```


### Snapshot of the Application

![Snapshot 1](image/snapshot_1.png)

![Snapshot 2](image/snapshot_2.png)


### Shortage and Improvements

1. Each worker node needs to invoke the model.py; need to make sure the model is available on each worker node

2. Better way to establish Hive connection from each worker node?

3. Records are inserted to Hive in the granularity of RDD, generating too many small files in Hive internal table's metastore


### References

1. Design Patterns for using foreachRDD (one of the output operations on DStream)
`https://spark.apache.org/docs/latest/streaming-programming-guide.html#output-operations-on-dstreams`

2. Example of using Spark SQL with DStream
`https://github.com/apache/spark/blob/v2.2.0/examples/src/main/python/streaming/sql_network_wordcount.py`

3. ETL Using Spark Streaming and Hive (Scala)
`https://vivekmangipudi.wordpress.com/2017/02/13/hive-etl-spark-streaming-with-hive/`