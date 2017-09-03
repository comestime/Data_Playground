from kafka import KafkaClient, SimpleProducer
import requests
import time
import sys

def send_messages(producer):
	while (1):
		r = requests.get("http://fintech.dataapplab.com:33334/api/v1.0/FinTech/streamingdata",
						 stream = True)

		# for line in r.iter_lines():
		#	producer.send_messages('fintech_pyspark', line)
		# 	print line

		# cannot use r.json(), since python expects property names enclosed in double quotes
		line = str(r.text.replace('\n', '').replace('  ', ' ').replace('  ', ' '))
		# specify topic name
		producer.send_messages('fintech_pyspark', line)
		print type(line)
		print line

		time.sleep(5)


if __name__ == "__main__":
	if len(sys.argv) < 1:
		print("Usage: python source.py <mode>")
		exit(-1)

	mode = sys.argv[1]
	if mode == 'local':
		kafka = KafkaClient('localhost:9092')
	else:
		kafka = KafkaClient('m1.mt.dataapplab.com:6667')

	producer = SimpleProducer(kafka)
	send_messages(producer)
	kafka.close()