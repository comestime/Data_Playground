import os
import json
import pickle
import cPickle as cpickle
# TEMP FIX
# import xgboost as xgb
import random

class Model():

	def __init__(self):
		pass
	
	def getPrediction(self, input_json):

		trainedModel = os.path.join(os.getcwd(), '', 'model.pkl')
		fPath        = os.path.join(os.getcwd(), '', 'feature.pkl')

		# TEMP FIX
		# feature = pickle.load(open(fPath, 'rb'))
		# model   = cpickle.load(open(trainedModel, 'r'))
		feature = {}
	
		# fix feature
		feature['bc_open_to_buy'] = input_json["bc_open_to_buy"]
		feature['total_il_high_credit_limit'] = input_json["total_il_high_credit_limit"]
		feature['dti'] = input_json["dti"]
		feature['annual_inc'] = input_json["annual_inc"]
		feature['bc_util'] = input_json["bc_util"]

		feature['int_rate'] = float(input_json["int_rate"] / 100)
		# high wie
		if input_json["term"] == 36:
			feature['term_ 36 months'] = 1
			feature['term_ 60 months'] = 0
		else:
			feature['term_ 36 months'] = 0
			feature['term_ 60 months'] = 1

		feature['loan_amnt'] = input_json["loan_amnt"]

		loan_rate = float(input_json["fund_rate"] / 100)
		feature['funded_amnt'] = float(loan_rate * feature['loan_amnt'])

		# print type(feature)
		# print feature
		# TEMP FIX
		# dtrain = xgb.DMatrix(feature, missing=np.NAN)
		# print (dtrain)

		# TEMP FIX
		# result= model.predict(dtrain)
		result = random.uniform(0, 1)
		status = "success";
		# TEMP FIX
		data = round(result, 3)

		return {"data": data, "status": status}


