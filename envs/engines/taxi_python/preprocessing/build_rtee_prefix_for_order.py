from rtree import index
import pandas as pd
from os.path import join
from Core.settings import DATA_PATH, RIDE_PATH, TRACK_PATH
import sys
import time
import pickle
import numpy as np
import math

try:
    date = int(sys.argv[1])
except IndexError:
    date = 20161106


# # build rtree
df_rides = pd.read_csv(join(DATA_PATH, RIDE_PATH, "order_" + str(date)), header=None)
df_rides.drop_duplicates(subset=[0], keep='first', inplace=True)
rides_info = df_rides.values

order_route = pickle.load(open(join(DATA_PATH, TRACK_PATH, "order_route_" + str(date)), "rb"))

MAX_T = 86402

file_idx = index.Rtree(join(DATA_PATH, RIDE_PATH, 'rtree' + str(date)))
index2hash = []
order_prefix = dict()

for i in range(df_rides.shape[0]):
    if i % 10000 == 0:
        print(i)
    hashcode, start_timestamp, finish_timestamp, start_lng, start_lat, finish_lng, finish_lat, reward \
        = rides_info[i][0:8]
    if start_lng * start_lat * finish_lng * finish_lat < 0.5 or finish_timestamp - start_timestamp < 60:
        continue
    start_tm = time.localtime(start_timestamp)
    start_time = start_tm.tm_hour * 3600 + start_tm.tm_min * 60 + start_tm.tm_sec
    if start_time < 4 * 3600:
        continue
    finish_tm = time.localtime(finish_timestamp)
    finish_time = finish_tm.tm_hour * 3600 + finish_tm.tm_min * 60 + finish_tm.tm_sec
    index2hash.append(hashcode)
    file_idx.insert(len(index2hash) - 1, (start_time, MAX_T - finish_time, start_time, MAX_T - finish_time))

    route = order_route[hashcode]
    order_prefix[hashcode] = np.zeros(len(route) + 1, dtype=int)
    order_prefix[hashcode][0:2] = np.array([start_time, finish_time])
    inter = [abs(route[k][0] - route[k - 1][0]) + abs(route[k][1] - route[k - 1][1]) for k in range(1, len(route))]
    denom = sum(inter)
    inter = [round(each / denom * (finish_time - start_time)) for each in inter]
    order_prefix[hashcode][2:] = np.cumsum(inter)
    errorfinish = order_prefix[hashcode][-1]
    if abs(errorfinish - (finish_time - start_time)) > 10:
        print(errorfinish, finish_time - start_time)
    order_prefix[hashcode][-1] = finish_time - start_time # fix type convert error

pickle.dump(index2hash, open(join(DATA_PATH, RIDE_PATH, "index2hash" + str(date)), "wb"))
pickle.dump(order_prefix, open(join(DATA_PATH, TRACK_PATH, "order_prefix" + str(date)), "wb"))
