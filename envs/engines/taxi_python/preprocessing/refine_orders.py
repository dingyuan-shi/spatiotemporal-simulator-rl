import pandas as pd
from os.path import join
import pickle
from Core.settings import DATA_PATH
import sys
import json

CANCEL_PATH = 'total_order_cancellation_probability'
RIDE_PATH = 'total_ride_request'
TRACK_PATH = 'total_ride_track'

try:
    day = int(sys.argv[1])
except IndexError:
    day = 20161106


df_rides = pd.read_csv(join(DATA_PATH, RIDE_PATH, "order_" + str(day)), header=None)
df_rides.drop_duplicates(subset=[0], keep='first', inplace=True)
df_cancel_prob = pd.read_csv(join(DATA_PATH, CANCEL_PATH, "order_%8d_cancel_prob" % day), header=None)
df_cancel_prob.drop_duplicates(subset=[0], keep='first', inplace=True)
df_rides_info = pd.merge(df_rides, df_cancel_prob, on=0)
rides_info = df_rides_info.sort_values(by='1_x').values
pickle.dump(rides_info, open(join(DATA_PATH, RIDE_PATH, "order_with_cancel" + str(day)), "wb"))
rides_info_json = []
for i in range(rides_info.shape[0]):
    entry = rides_info[i]
    entry_dict = {
        'order_id': entry[0],
        'begin_timestamp': entry[1],
        'end_timestamp': entry[2],
        'begin_lng': entry[3],
        'begin_lat': entry[4],
        'end_lng': entry[5],
        'end_lat': entry[6],
        'reward': entry[7],
        'cancel_prob': list(entry[8:])
    }
    rides_info_json.append(entry_dict)
json.dump(rides_info_json, open(join(DATA_PATH, RIDE_PATH, "order_with_cancel" + str(day) + ".json"), "w"))
