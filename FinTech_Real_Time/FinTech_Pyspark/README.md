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
 

