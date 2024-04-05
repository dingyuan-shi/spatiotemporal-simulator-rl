import pickle
from .grids import Grids
import random
from os.path import join
from ..settings import seed, DATA_PATH, SPEED, IS_REPO_CAN_SERVE, IDLE_TRANSITION_FREQUENCY, \
    REPO_TRANSITION_FREQUENCY, IS_TRACK
from enum import Enum
from .utils import acc_dist, transition
from collections import namedtuple
import numpy as np
random.seed(seed)
np.random.seed(seed)

repo_routes = pickle.load(open(join(DATA_PATH, "repo_route"), "rb"))


class Driver:
    global_number = 0  # 全局编号，每个司机用独一无二的id
    driver_model = pickle.load(open(join(DATA_PATH, "driver_model"), "rb"))
    log_off_prob = driver_model['log_off']  # 下线的概率

    class State(Enum): # 司机的状态
        OFF = 0    # 下线
        IDLE = 1   # 空车
        REPO = 2   # 被调度
        SERVE = 3  # 接单状态

    def __init__(self, cur_time=0, lng=0.0, lat=0.0, grid_id="", can_be_repo=True):
        # 初始化：指定刚上线的司机的地点
        self.log_on_time, self.lng, self.lat, self.state = cur_time, lng, lat, Driver.State.IDLE
        self.grid = grid_id
        self.timer = 0
        self.t2 = 0
        self.p = 0
        self.serve_info = ["", -1]
        Driver.global_number += 1
        self.hashcode = str(Driver.global_number)
        self.no = Driver.global_number
        self.can_be_repo = can_be_repo
        self.tracks = []
        self.is_assign = True

    def log_off(self, cur_time_seg):
        # 下线操作
        if self.state == Driver.State.IDLE and random.random() < Driver.log_off_prob[cur_time_seg]:
            self.state = Driver.State.OFF
            return True
        return False

    def repo_to(self, grid_id):
        # 调度司机去指定的六边形网格
        self.state = Driver.State.REPO
        self.timer = 0
        self.p = 1
        if (self.grid, grid_id) in repo_routes:
            self.tracks = repo_routes[(self.grid, grid_id)]
        else:
            f_lng, f_lat = Grids.get_grid_location(grid_id)
            self.tracks = [(self.lng, self.lat), (f_lng, f_lat)]
        self.t2 = int(acc_dist(*self.tracks[0], *self.tracks[1]) / SPEED)
        if not IS_REPO_CAN_SERVE:
            self.is_assign = False

    def assign_order(self, order):
        # 给司机分配订单
        self.state = Driver.State.SERVE
        self.timer = 0
        self.t2 = order.finish_time - order.start_time
        self.is_assign = False
        if IS_TRACK:
            self.tracks = order.track
        else:
            self.tracks = [(order.finish_lng, order.finish_lat)]
        self.serve_info = [order.hashcode, 0]
        if len(self.tracks) == 0:
            print()

    def update(self, cur_sec):
        # 更新司机的位置
        self.timer += 2
        if self.state == Driver.State.IDLE:
            if self.timer % IDLE_TRANSITION_FREQUENCY // 2 == 0:
                self.lng, self.lat = Grids.gen_random(self.grid)
            elif self.timer % IDLE_TRANSITION_FREQUENCY == 0:
                self.grid = transition(cur_sec, self.grid)
                self.lng, self.lat = Grids.gen_random(self.grid)
        elif self.state == Driver.State.REPO:
            if self.timer % REPO_TRANSITION_FREQUENCY == 0:
                if self.timer < self.t2:
                    k = self.timer / self.t2
                    self.lng = (1 - k) * self.tracks[self.p - 1][0] + k * self.tracks[self.p][0]
                    self.lat = (1 - k) * self.tracks[self.p - 1][1] + k * self.tracks[self.p][1]
                    self.grid = Grids.find_grid_by_current(self.lng, self.lat, self.grid)
                else:
                    self.p += 1
                    if self.p >= len(self.tracks):
                        self.state = Driver.State.IDLE
                        self.timer = 0
                        self.is_assign = True
                    else:
                        self.t2 = int(acc_dist(*self.tracks[self.p - 1], *self.tracks[self.p]) / SPEED)
        elif self.state == Driver.State.SERVE:
            if self.timer < self.t2:
                self.lng, self.lat = self.tracks[int(self.timer / self.t2 * len(self.tracks))]
            else:
                self.state = Driver.State.IDLE
                self.timer = 0
                self.serve_info[1] = 2
                self.grid = Grids.find_grid(self.lng, self.lat)
                self.is_assign = True
        else:
            print("Warning: detect offline worker in update!")

    def can_be_repositioned(self, repo_freq):
        # 司机能否被调度，空车和距离上次调度过去REPO_FREQUENCY时间的司机可以被调度
        return self.can_be_repo and (self.state == Driver.State.IDLE or
                                     (self.state == Driver.State.REPO and self.timer % repo_freq == 0))

    def get_location(self):
        # 获取司机位置
        return self.lng, self.lat

    def get_grid(self):
        # 获取司机所在六边形网格
        return self.grid

    def get_info(self):
        if self.serve_info[1] == 0:
            order_id, duration = self.serve_info[0], self.t2
            self.serve_info[1] = 1
            return order_id, duration
        elif self.serve_info[1] == 2:
            order_id = self.serve_info[0]
            self.serve_info[1] = -1
            return order_id, 0
        return "", -1

    def __str__(self):
        res = "DRIVER: " + self.hashcode + " "
        if self.can_be_repo:
            res += "*"
        else:
            res += "-"
        res += "at (%.7f, %.7f) state:%s for %d s" % (self.lng, self.lat, self.state, self.timer)


Order = namedtuple('Order', ['hashcode', 'start_lng', 'start_lat', 'finish_lng', 'finish_lat',
                             'start_time', 'finish_time', 'start_grid', 'reward', 'cancel_prob', 'track'])

DGPair = namedtuple('DGPair', ['driver_id', 'grid_id'])

RepoInfo = namedtuple('RepoInfo', ['timestamp', 'driver_info', 'day_of_week'])

DispatchElem = namedtuple('DispatchElem', ['order_id', 'driver_id', 'order_driver_distance', 'order_start_location',
                                           'order_finish_location', 'driver_location', 'timestamp',
                                           'order_finish_timestamp', 'day_of_week', 'reward_units', 'pick_up_eta'])
