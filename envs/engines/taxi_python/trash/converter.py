import pickle
import csv
from os.path import join
from Core.settings import DATA_PATH
from collections import defaultdict
import sys
import json

try:
    date = int(sys.argv[1])
except IndexError:
    date = 20161106

min_t, max_t = 100000, -100
order_route = pickle.load(open(join(DATA_PATH, 'total_ride_track', 'order_points_' + str(date)), "rb"))

avail_driver = csv.reader(open(join(DATA_PATH, 'avail_driver_rec' + str(date) + '.csv')), delimiter=',')
avail_driver_dict = defaultdict(list)
for row in avail_driver:
    t, lng, lat = row
    min_t = min(int(t), min_t)
    max_t = max(int(t), max_t)
    avail_driver_dict[int(t)].append([float(lng), float(lat)])

driver_track = csv.reader(open(join(DATA_PATH, 'driver_track_rec' + str(date) + '.csv')), delimiter=',')
driver_track_dict = defaultdict(list)
show_up = set()
i = 0
for row in driver_track:
    i += 1
    t, driver_id, order_id = row
    min_t = min(int(t), min_t)
    max_t = max(int(t), max_t)
    if (driver_id, order_id) not in show_up:
        show_up.add((driver_id, order_id))
        # order_route[order_id].flatten().tolist()
        driver_track_dict[int(t)].append({'driver_id': driver_id, 'order_id': order_id, 'route': order_route[order_id].tolist()})
    else:
        driver_track_dict[int(t)].append({'driver_id': driver_id, 'order_id': order_id})
    if i % 10000 == 0:
        print(i)

order_loc = csv.reader(open(join(DATA_PATH, 'order_rec' + str(date) + '.csv')), delimiter=',')
order_loc_dict = defaultdict(list)
for row in order_loc:
    t, lng, lat = row
    min_t = min(int(t), min_t)
    max_t = max(int(t), max_t)
    order_loc_dict[int(t)].append([float(lng), float(lat)])

stats = csv.reader(open(join(DATA_PATH, 'stats_rec' + str(date) + '.csv')), delimiter=',')
stats_dict = defaultdict(dict)
for row in stats:
    t, total_driver_num, avail_driver_num, order_num, ans_rate, comp_rate, matching_time, repo_time, \
    seg_rewards, accu_rewards = row
    min_t = min(int(t), min_t)
    max_t = max(int(t), max_t)
    stats_dict[int(t)] = {'total_driver_num': int(total_driver_num), 'avail_driver_num': int(avail_driver_num),
                         'order_num': int(order_num), 'ans_rate': float(ans_rate), 'comp_rate': float(comp_rate),
                         'matching_time': float(matching_time), 'repo_time': float(repo_time), 'seg_rewards': float(seg_rewards),
                          'accu_rewards': accu_rewards}

jsons = dict()
for t in range(min_t, max_t + 1, 2):
    if t % 100 == 0:
        print(t)
    jsons[t] = json.dumps({'order_heat': order_loc_dict[t], 'driver_heat': avail_driver_dict[t],
                           'track': driver_track_dict[t], 'stats': stats_dict[t]})

pickle.dump(jsons, open(join(DATA_PATH, "vision_json_" + str(date)), "wb"))
