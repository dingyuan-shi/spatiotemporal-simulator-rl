import pickle
import sys
import os
import dask.dataframe as dd 
from collections import defaultdict
from Core.settings import DATA_PATH
import numpy as np

try:
    date = sys.argv[1]
except IndexError:
    date = '20161106'

src_dir = os.path.join(DATA_PATH, 'gps_' + date)
dst_dir = os.path.join(DATA_PATH, 'total_ride_track', 'order_track_' + date)

gps_data = dd.read_csv(src_dir, header=None)
order_to_track = defaultdict(list)
print('processing %s gps data...' % date)

cnt = 0
for row in gps_data.itertuples(index=False):
    cnt += 1
    if cnt % 1000000 == 0:
        print(cnt)
    driver_id, order_id, ts, lng, lat = row
    order_to_track[order_id].append((int(ts), float(lng), float(lat)))

for order in order_to_track:
    order_to_track[order].sort()
    order_to_track[order] = np.array(order_to_track[order])
    order_to_track[order][:, 0].astype(int)
pickle.dump(order_to_track, open(dst_dir, 'wb'))
