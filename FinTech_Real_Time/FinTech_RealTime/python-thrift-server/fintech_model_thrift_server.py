from FinTech import FinTech
from thrift.transport import TSocket
from thrift.transport import TTransport
from thrift.protocol import TBinaryProtocol
from thrift.server import TServer

import os
import json
import pickle
import cPickle as cpickle
import xgboost as xgb
import numpy as np

class Model():

	def __init__(self):
		pass
	
	def getPrediction(self, input_str):
		input_json = json.loads(input_str)

		trainedModel = os.path.join(os.getcwd(),'','model.pkl')
   		fPath        = os.path.join(os.getcwd(),'','feature.pkl')

		feature = pickle.load(open(fPath, 'rb'))
		model   = cpickle.load(open(trainedModel, 'r'))
	
		# fix feature
		feature['bc_open_to_buy'] = float(input_json["bc_open_to_buy"])
		feature['total_il_high_credit_limit'] = float(input_json["total_il_high_credit_limit"])
		feature['dti'] = float(input_json["dti"])
   		feature['annual_inc'] = float(input_json["annual_inc"])
		feature['bc_util'] = float(input_json["bc_util"])

		feature['int_rate'] = float(float(input_json["int_rate"])/100)
		# high wie
		if int(input_json["term"]) == 36:
			feature['term_ 36 months'] = 1
			feature['term_ 60 months'] = 0
   		else:
			feature['term_ 36 months'] = 0
			feature['term_ 60 months'] = 1

		feature['loan_amnt'] = float(input_json["loan_amnt"])

   		loan_rate = float(float(input_json["fund_rate"])/100)
		feature['funded_amnt'] = float(loan_rate * feature['loan_amnt'])

		# print type(feature)
		# print feature
		dtrain = xgb.DMatrix(feature, missing=np.NAN)
		print (dtrain)
		
		result= model.predict(dtrain)
		status = "success";
		data = round(result[0], 3) 

		return '{"data": "' + str(data) + '", "status": "' + status + '"}'

if __name__ == "__main__":
	# model processor class
	model = Model()
	processor = FinTech.Processor(model)
	transport = TSocket.TServerSocket("127.0.0.1", 8989)
	# using buffer to do the transfer
	tfactory = TTransport.TBufferedTransportFactory()
	# transmission data type: binary
	pfactory = TBinaryProtocol.TBinaryProtocolFactory()
	# create a thrift service
	server = TServer.TSimpleServer(processor, transport, tfactory, pfactory)

	print "Starting thrift server in python..."
	server.serve()
	print "done!"

