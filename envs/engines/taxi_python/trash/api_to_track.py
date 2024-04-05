import pickle
from Core.settings import DATA_PATH
from os.path import join
import sys
import pandas as pd
import numpy as np

try:
    date = sys.argv[1]
except IndexError:
    date = '20161106'


src_dir = join(DATA_PATH, 'total_ride_track', 'order_route_' + date)
dst_dir = join(DATA_PATH, 'total_ride_track', 'order_points_' + date)
RIDE_PATH = 'total_ride_request'


df_rides = pd.read_csv(join(DATA_PATH, RIDE_PATH, "order_" + date), header=None)
df_rides.drop_duplicates(subset=[0], keep='first', inplace=True)
# build dictionary
order_to_duration = dict()
for index, row in df_rides.iterrows():
    if row[3] * row[4] * row[5] * row[6] < 0.5:
        continue
    order_to_duration[row[0]] = row[2] - row[1]

order_to_route = pickle.load(open(src_dir, "rb"))
# since now you have order_to_route and order_to_duration
order_to_points = dict()
cnt = 0
for order_hashcode in order_to_duration:
    cnt += 1
    duration = order_to_duration[order_hashcode]
    # begin to intercept
    route = order_to_route[order_hashcode]
    lng_lats = np.array(route).reshape(-1, 2)
    inter = np.zeros(lng_lats.shape[0] - 1)
    for i in range(1, lng_lats.shape[0]):
        inter[i - 1] = abs(lng_lats[i][0] - lng_lats[i - 1][0]) + abs(lng_lats[i][1] - lng_lats[i - 1][1])
    inter_num = (inter / np.sum(inter) * duration / 2).astype(int)
    points = []
    for i in range(1, lng_lats.shape[0]):
        point_num = inter_num[i - 1]
        points.extend((1. - k / point_num) * lng_lats[i - 1] + k / point_num * lng_lats[i] for k in range(point_num))
    order_to_points[order_hashcode] = np.array(points)
    if cnt % 1000 == 0:
        print(len(order_to_points))
pickle.dump(order_to_points, open(dst_dir, "wb"))
