from kafka import KafkaClient, SimpleProducer
import requests
import time

def send_messages(producer):
	while (1):
		r = requests.get("http://jupyter.datalaus.net:33334/api/v1.0/FinTech/streamingdata",
					 stream=True)

		# for line in r.iter_lines():
		#	producer.send_messages('fintech', line)
		# 	print line

		line = str(r.text.replace('\n', '').replace('  ', ' ').replace('  ', ' '))
		producer.send_messages('fintech', line)
		print type(line)
		print line

		time.sleep(5)


if __name__ == "__main__":
	kafka = KafkaClient('localhost:9092')
	producer = SimpleProducer(kafka)
	send_messages(producer)
	kafka.close()