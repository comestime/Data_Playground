import json
import sys

from pyspark import SparkContext
from pyspark.streaming import StreamingContext
from pyspark.streaming.kafka import KafkaUtils

# import trained model
from model import Model
model = Model()


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
    """utilize the trained model to perform prediction, and return results as JSON format"""
    prediction_json = model.getPrediction(x)
    result_json = { 'member_id' : int(x['member_id']),
                    'annual_inc' : int(x['annual_inc']),
                    'funded_amnt' : int(x['funded_amnt']),
                    'data' : float(prediction_json['data']),
                    'status' : prediction_json['status']}
    return result_json

if __name__ == "__main__":

    if len(sys.argv) < 2:
        print("Usage: spark_meetup.py <zk> <topic>")
        exit(-1)

    sc = SparkContext("local[2]",appName="PythonStreamingFinTech")
    sc.setLogLevel("WARN")
    ssc = StreamingContext(sc, 2)
    ssc.checkpoint("checkpoint")

    zkQuorum, topic = sys.argv[1:]
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
    strm_json_trained.pprint()


    ssc.start()
    ssc.awaitTermination()
