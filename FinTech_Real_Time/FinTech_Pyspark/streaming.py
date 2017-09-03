import json
import sys
import requests

from pyspark import SparkContext
from pyspark.sql import SparkSession
from pyspark.streaming import StreamingContext
from pyspark.streaming.kafka import KafkaUtils
from pyspark.sql import HiveContext, Row

# import trained model
# from model import Model
# model = Model()


def convert_numbers(x):
    if x.isdigit():
        return x
    else:
        return 0


def strm_json_extraction(x):
    """Extract required fields from incoming Json stream, and do some data cleaning"""
    return_json = { 'member_id': int(x["member_id"]),
                    'bc_open_to_buy': int(convert_numbers(x["bc_open_to_buy"])),
                    'total_il_high_credit_limit': int(convert_numbers(x["total_il_high_credit_limit"])),
                    'dti': 0,
                    'annual_inc': int(convert_numbers(x["annual_inc"])),
                    'bc_util': int(convert_numbers(x["bc_util"])),
                    'int_rate': float(x["int_rate"].replace("%", "")),
                    'term': int(x["term"].replace(" months", "")),
                    'loan_amnt': int(x["loan_amnt"]),
                    'fund_rate': 0,
                    'funded_amnt': int(x["funded_amnt"])}
    return return_json


def strm_json_training(x):
    """Utilize the trained model to perform prediction, and return results as JSON format"""
    # prediction_json = model.getPrediction(x)
    r = requests.post('http://fintech.dataapplab.com:33334/api/v1.0/FinTech', json = x)
    print r.headers
    prediction_json = r.json()
    print(prediction_json)
    result_json = { 'member_id' : int(x['member_id']),
                    'annual_inc' : int(x['annual_inc']),
                    'funded_amnt' : int(x['funded_amnt']),
                    'data' : float(prediction_json['data']),
                    'status' : prediction_json['status']}
    return result_json


def getSparkSessionInstance(sparkConf):
    """get sparkSession from rdd sparkConf"""
    if ('sparkSessionSingletonInstance' not in globals()):
        globals()['sparkSessionSingletonInstance'] = SparkSession\
            .builder\
            .config(conf=sparkConf)\
            .getOrCreate()
    return globals()['sparkSessionSingletonInstance']


def save2hive(time, rdd):
    print("========= %s =========" % str(time))

    try:
        # Get the singleton instance of SparkSession
        spark = getSparkSessionInstance(rdd.context.getConf())

        # Get hive context
        hiveCtx = HiveContext(spark)

        # Convert RDD[String] to RDD[Row] to DataFrame
        rowRdd = rdd.map(lambda x : Row(member_id=x['member_id'],
                                        annual_inc=x['annual_inc'],
                                        funded_amnt=x['funded_amnt'],
                                        data=x['data'],
                                        status=x['status']))
        resultDF = spark.createDataFrame(rowRdd)

        # Creates a temporary view using the DataFrame
        resultDF.createOrReplaceTempView("result")
        resultDF.show()

        # Perform the insertion
        hiveCtx.sql("INSERT INTO TABLE prediction SELECT * FROM result")
        hiveCtx.sql("SELECT * FROM prediction").show()
    except:
        pass


if __name__ == "__main__":

    if len(sys.argv) < 2:
        print("Usage: streaming.py <mode> <topic>")
        exit(-1)

    mode, topic = sys.argv[1:]
    if mode == 'local':
        zkQuorum = 'localhost:2181'
    else:
        zkQuorum = 'm1.mt.dataapplab.com:2181'

    sc = SparkContext("local[2]", appName="PythonStreamingFinTech")
    sc.setLogLevel("WARN")
    ssc = StreamingContext(sc, 2)
    ssc.checkpoint("checkpoint")
    hiveCtx = HiveContext(sc)
    print "[DEBUG] Creating Hive Table..."
    if mode == 'cluster':
        print "[DEBUG] Use alic_db in cluster mode"
        hiveCtx.sql("USE alick_db")
    if mode == 'local':
        print "[DEBUG] Drop table 'Prediction' in local mode"
        hiveCtx.sql("DROP TABLE IF EXISTS prediction")
	# need to match the field order in TempView "result" since the hive insertion is a simple write append to the file
    hiveCtx.sql("CREATE TABLE IF NOT EXISTS prediction \
                (annual_inc INT, data FLOAT, funded_amnt INT, member_id INT, status STRING)")

    kvs = KafkaUtils.createStream(ssc, zkQuorum, "1", {topic: 1})

    # kvs.pprint()
    # first element is 'None'
    strm = kvs.map(lambda x: x[1])
    # convert the stream to JSON objects
    strm_json = strm.map(lambda x: json.loads(x.encode('ascii','ignore')))
    # extract the required fields for prediction
    strm_json_extracted = strm_json.map(strm_json_extraction)
    # perform the prediction and get prediction results
    strm_json_trained = strm_json_extracted.map(strm_json_training)
    # strm_json_trained.pprint()
    strm_json_trained.foreachRDD(save2hive)


    ssc.start()
    ssc.awaitTermination()
