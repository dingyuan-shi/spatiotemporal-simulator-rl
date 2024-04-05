import copy
import json
import os
from os.path import join
import pickle
import numpy as np
import rtree
import random
from flask import Flask, render_template
from flask_sock import Sock
# from geventwebsocket import WebSocketError
# from geventwebsocket.handler import WebSocketHandler
# from gevent.pywsgi import WSGIServer


WORK_DIR = join(os.environ['DSS_DIR'], "workdir")
DATA_PATH = join(WORK_DIR, "data/taxiData")
REC_PATH = join(WORK_DIR, "result/testTaxi")
RIDE_PATH = "total_ride_request"
TRACK_PATH = 'total_ride_track'

random.seed(0)
drop_ratio = 0.8

app = Flask(__name__)
sock = Sock()
sock.init_app(app)

@app.route("/")
def index():
    return render_template("index.html")

@sock.route("/message")
def message(ss):
    # 初始化发送消息
    records_by_time, serve_orders = pickle.load(open(join(REC_PATH, "record"), "rb"))
    # track
    date = "20161106"
    order_route = pickle.load(open(join(DATA_PATH, TRACK_PATH, "order_route_" + str(date)), "rb"))
    index2hash = pickle.load(open(join(DATA_PATH, RIDE_PATH, "index2hash" + str(date)), "rb"))
    order_prefix = pickle.load(open(join(DATA_PATH, TRACK_PATH, "order_prefix" + str(date)), "rb"))
    file_idx = rtree.index.Rtree(join(DATA_PATH, RIDE_PATH, "rtree20161106"))
    demand = []
    supply = []
    first_order_heat = None
    first_driver_heat = None
    for t in range(4 * 3600, 24 * 3600, 2):
        if 'order_heat' in records_by_time[t]:
            if len(records_by_time[t]['order_heat']) > 0:
                if first_order_heat is None:
                    first_order_heat = records_by_time[t]['order_heat']
                demand.append(len(records_by_time[t]['order_heat']))
        if 'driver_heat' in records_by_time[t]:
            if len(records_by_time[t]['driver_heat']) > 0:
                if first_driver_heat is None:
                    first_driver_heat = records_by_time[t]['driver_heat']
                supply.append(len(records_by_time[t]['driver_heat']))
    if len(demand) < len(supply):
        demand.insert(0, 0)
    ss.send(json.dumps({'demand': demand, 'supply': supply}))
    # 接收消息并返回
    while True:
        try:
            text_data = ss.receive()
            text_data_dict = json.loads(text_data)
            flag = text_data_dict['flag']
            query_t = int(float(text_data_dict['query_t']))
            if query_t % 2 == 1:
                query_t -= 1

            # flag == 2 代表拖拉查询
            if flag == 2:
                res = {'order_heat': records_by_time[query_t]['order_heat'],
                    'driver_heat': records_by_time[query_t]['driver_heat'],
                    'stats': records_by_time[query_t]['stats'], 'track': []}

                if len(res['order_heat']) == 0:
                    # 当前查询没有对应order_heat数据的时候,向后查询到第一次
                    for t in range(query_t, 86402, 2):
                        if len(records_by_time[t]['order_heat']) != 0:
                            first_order_heat = records_by_time[t]['order_heat']
                            first_driver_heat = records_by_time[t]['driver_heat']
                            res['order_heat'] = records_by_time[t]['order_heat']
                            res['driver_heat'] = records_by_time[t]['driver_heat']
                            break
                    # 当前查询没有对应stats,向前查询
                    for t in range(query_t, 0, -2):
                        if len(records_by_time[t]['stats']) != 0:
                            res['stats'] = records_by_time[t]['stats']
                            break
                # (left,bottom,right,top)
                # 相当于查询start_time < query_t && finish_time > query_time的所有点
                # 查询orders
                order_hashcodes = [index2hash[each]
                                for each in file_idx.intersection((0, 0, query_t, 86402 - query_t))]
                for hashcode in order_hashcodes:
                    # 只有服务过的order才记录
                    if hashcode not in serve_orders:
                        continue
                    # t1:start_time,t2:finish_time
                    t1, t2 = order_prefix[hashcode][0: 2]
                    # prefix:[完成第1步需要的时间,完成第1/2步需要的时间,完成第1/2/3步需要的时间,...]
                    prefix = order_prefix[hashcode][2:]
                    if query_t >= t2 or query_t < t1 or random.random() < drop_ratio:
                        continue
                    else:
                        # 找到第一个在查询时间之前的idx
                        idx = np.searchsorted(prefix, query_t - t1)
                        if prefix[idx] != query_t - t1:
                            real_route = copy.deepcopy(order_route[hashcode][idx:])
                            k = (query_t - t1) / prefix[-1]
                            real_route[0][0] = (1 - k) * real_route[0][0] + k * real_route[1][0]
                            real_route[0][1] = (1 - k) * real_route[0][1] + k * real_route[1][1]
                        else:
                            real_route = copy.deepcopy(order_route[hashcode][idx + 1:])
                    res['track'].append({'order_id': hashcode, 'duration': int(t2 - query_t), 'route': real_route})
            # flag == 1,代表每隔两秒的查询
            else:
                res = records_by_time[query_t]
                sample_track = []
                for track in res['track']:
                    track['duration'] = int(track['duration'])
                    if track['duration'] <= 0:
                        sample_track.append(track)
                    else:
                        if random.random() < drop_ratio:
                            continue
                        track['route'] = order_route[track['order_id']]
                        sample_track.append(track)
                if len(res['order_heat']) == 0:
                    res['order_heat'] = first_order_heat
                else:
                    first_order_heat = res['order_heat']
                if len(res['driver_heat']) == 0:
                    res['driver_heat'] = first_driver_heat
                else:
                    first_driver_heat = res['driver_heat']
                res['track'] = sample_track
            res['flag'] = flag
            ss.send(json.dumps(res))
        except Exception as e:
            print(e)
    
    

# if __name__ == '__main__':
#     # Note: please use flask under 1.0.0 version
#     http_server = WSGIServer(('localhost', 5000), app, handler_class=WebSocketHandler)
#     http_server.serve_forever()
