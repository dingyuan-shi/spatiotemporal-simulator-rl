import random
import math
import time
import pickle
from os.path import exists, join
from os import remove
import copy
from typing import Dict

from ..settings import seed, begin_day, end_day, SPEED, LOG_FREQUENCY, REPO_FREQUENCY, DATA_PATH, REC_PATH
from ..model.agent import Agent
from ..simulator.dataloader import DataLoader
from ..simulator.utils import acc_dist, convert_to_timestamp, sec_to_human
from ..simulator.grids import Grids
from ..simulator.driver_group import DriverGroup
from ..simulator.entity import Order, DGPair, RepoInfo, DispatchElem
from collections import defaultdict

random.seed(seed)


class Simulator:
    def __init__(self, agent=None):
        self.day = -1
        self.dow = -1
        self.agent = agent
        self.statistics = {'total_driver_num': 0, 'avail_driver_num': 0, 'order_num': 0,
                           'ans_rate': 0, 'comp_rate': 0, 'matching_time': 0, 'repo_time': 0, 
                           'seg_rewards': 0, 'accu_rewards': 0}
        self.records_by_time = defaultdict(dict)
        self.serve_orders = set()
        # 记录了所有的性能指标，分别为
        # total_driver_num 司机总数、avail_driver_num：可接单司机数量、order_num：这段时间的订单数量
        # ans_rate：应答率，即你分配了司机的订单占全部订单的比例
        # comp_rate：完成率，因为分配了可能被拒单，这个是最后没有拒单的订单占全部订单的比例
        # matching_time：匹配算法执行时间
        # repo_time：调度算法执行时间
        # seg_rewards：统计的时间段的订单收益
        # accu_rewards：累计订单总收益
        self.records_by_time_dir = ""
        self.processing_dir = ""

    def update_day(self, day, start_time, finish_time):
        if day != self.day:
            self.day = day
            self.records_by_time_dir = join(DATA_PATH, REC_PATH, "records_" + str(self.day) + "_"
                                            + str(int(round(time.time() * 1000))))
            self.processing_dir = join(DATA_PATH, REC_PATH, "tmp.log")
            if exists(self.processing_dir):
                remove(self.processing_dir)
            DriverGroup.clear()
            print("loading from day%02d..." % day)
            DataLoader.load_data(day)
            DataLoader.skip_to(start_time)
            online_drivers = DriverGroup.init_drivers(start_time)
            print("finish loading, begin simulate")
            for name in self.statistics:
                self.statistics[name] = 0
            self.sec = 0
            self.start_time = start_time
            self.finish_time = finish_time
            self.chunk_orders = []
            
    def update_day_reset(self, day, start_time, finish_time):
        self.day = day
        self.records_by_time_dir = join(DATA_PATH, REC_PATH, "records_" + str(self.day) + "_"
                                        + str(int(round(time.time() * 1000))))
        self.processing_dir = join(DATA_PATH, REC_PATH, "tmp.log")
        if exists(self.processing_dir):
            remove(self.processing_dir)
        DriverGroup.clear()
        print("loading from day%02d..." % day)
        DataLoader.load_data(day)
        DataLoader.skip_to(start_time)
        self.start_time = start_time
        self.finish_time = finish_time
        for name in self.statistics:
            self.statistics[name] = 0
        self.sec = start_time
        self.chunk_orders = []

    def build_dispatch_observ(self, sec, orders, grid_to_drivers, avail_driver_dict):
        # 根据当前的订单和司机，构建二分图
        timestamp = convert_to_timestamp(self.day, sec)
        dispatch_observ = []
        for order in orders:
            o_lng, o_lat = order.start_lng, order.start_lat
            for grid in Grids.find_grid_more(order.start_grid, k=6):
                for d_hashcode in grid_to_drivers[grid]:
                    d_lng_lat = avail_driver_dict[d_hashcode].get_location()
                    od_dist = acc_dist(o_lng, o_lat, *d_lng_lat)
                    if od_dist < 3000:
                        dispatch_observ.append(DispatchElem(order.hashcode, d_hashcode, od_dist, (o_lng, o_lat),
                                        (order.finish_lng, order.finish_lat), d_lng_lat, timestamp, order.finish_time,
                                     self.dow, order.reward, od_dist / SPEED))
        return dispatch_observ

    def build_repo_observ(self, cur_time, repo_drivers):
        # 根据当前可以调度的车及其信息，构建调度信息
        return RepoInfo(convert_to_timestamp(self.day, cur_time),
                        [DGPair(hashcode, grid) for lng, lat, grid, hashcode in repo_drivers], self.day % 7)

    @staticmethod
    def reject(candidate, order_hash_to_info: Dict[str, Order], avail_driver_dict):
        # 模拟拒单过程
        if type(candidate) != tuple:
            order_hash, driver_hash = candidate.order_id, candidate.driver_id
        else:
            order_hash, driver_hash = candidate
        seg = math.floor(acc_dist(*avail_driver_dict[driver_hash].get_location(),
                         order_hash_to_info[order_hash].start_lng, order_hash_to_info[order_hash].start_lat) / 200)
        if seg < 9:
            return random.random() < order_hash_to_info[order_hash].cancel_prob[seg]
        return random.random() < 0.9

    def start_simulation(self):
        self.chunk_orders = []
        for sec in range(self.start_time, self.finish_time + 1, 2):
            # 每2秒钟需要做如下5个步骤
            # STEP1 更新司机
            offline_drivers = DriverGroup.log_off(sec)
            self.agent.update_log_off(offline_drivers, sec)
            online_drivers = DriverGroup.log_on(sec)
            self.agent.update_log_on(online_drivers, sec)
            # STEP2 获取订单、执行分单算法
            # assignment
            orders = DataLoader.get_orders(sec)
            # 按照设定的频率更新统计信息
            if sec % LOG_FREQUENCY == 0:
                self.statistics.update(total_driver_num=DriverGroup.get_driver_numbers(),
                                       avail_driver_num=DriverGroup.get_avail_driver_numbers())
            # 这个2s有订单 则构建二分图 执行分单算法
            if orders:
                self.chunk_orders.extend(orders)
                self.statistics['order_num'] += len(orders)
                grid_to_drivers, avail_driver_dict = DriverGroup.get_assign_drivers()
                order_hash_to_info = {order.hashcode: order for order in orders}
                dispatch_observ = self.build_dispatch_observ(sec, orders, grid_to_drivers, avail_driver_dict)
                t1 = time.process_time()
                matching = self.agent.dispatch(dispatch_observ)
                t2 = time.process_time()
                real_match = set(candidate for candidate in matching
                                 if not Simulator.reject(candidate, order_hash_to_info, avail_driver_dict))
                self.serve_orders.update(set(candidate.order_id for candidate in real_match))
                self.agent.update_driver_income_after_rejection(real_match, dispatch_observ)
                driver_order = [(candidate.driver_id, order_hash_to_info[candidate.order_id])
                                for candidate in real_match]
                rewards = sum(order_hash_to_info[candidate.order_id].reward for candidate in real_match)
                DriverGroup.assign_order_to_drivers(driver_order)
                self.statistics['ans_rate'] += len(matching) / len(orders)
                self.statistics['comp_rate'] += len(real_match) / len(orders)
                self.statistics['matching_time'] += (t2 - t1)
                self.statistics['accu_rewards'] += rewards
                self.statistics['seg_rewards'] += rewards
            # STEP3 执行调度算法
            repo_drivers = DriverGroup.get_repo_drivers(sec, REPO_FREQUENCY)
            if repo_drivers:
                repo_observ = self.build_repo_observ(sec, repo_drivers)
                t1 = time.process_time()
                repo_result = self.agent.reposition(repo_observ)
                t2 = time.process_time()
                self.statistics['repo_time'] += (t2 - t1)
                DriverGroup.repo_drivers(repo_result)
            # STEP4 更新所有司机状态
            # update
            DriverGroup.update(sec)
            # STEP5 更新统计信息和轨迹相关信息用于可视化
            self.records_by_time[sec]['driver_heat'] = []
            self.records_by_time[sec]['order_heat'] = []
            self.records_by_time[sec]['stats'] = {}
            self.records_by_time[sec]['track'] = []
            for d_hash, driver in DriverGroup.drivers.items():
                order_id, duration = driver.get_info()
                if duration != -1:
                    self.records_by_time[sec]['track'].append({'driver_id': d_hash, 'order_id': order_id, 'duration': duration})

            if sec % LOG_FREQUENCY == 0:
                self.statistics['ans_rate'] /= (LOG_FREQUENCY / 2)
                self.statistics['comp_rate'] /= (LOG_FREQUENCY / 2)
                print("%s: %5d/%5d drivers, %5d orders, r_ans: %.3f, r_comp: %.3f, match time %.3f, repo time %.3f, "
                      "rewards: %.3f, total rewards: %.3f" %
                      (sec_to_human(sec), self.statistics['avail_driver_num'], self.statistics['total_driver_num'],
                       self.statistics['order_num'], self.statistics['ans_rate'], self.statistics['comp_rate'],
                       self.statistics['matching_time'], self.statistics['repo_time'], self.statistics['seg_rewards'],
                       self.statistics['accu_rewards']))
                with open(self.processing_dir, "a") as f:
                    f.writelines(str(sec) + "\n")
                for order in self.chunk_orders:
                    self.records_by_time[sec]['order_heat'].append([order.start_lng, order.start_lat])
                self.chunk_orders.clear()

                for d_hash, driver in DriverGroup.drivers.items():
                    if driver.is_assign:
                        self.records_by_time[sec]['driver_heat'].append([*driver.get_location()])

                self.records_by_time[sec]['stats'] = copy.copy(self.statistics)
                self.statistics.update(order_num=0, ans_rate=0, comp_rate=0, matching_time=0, repo_time=0,
                                       seg_rewards=0)
        pickle.dump((self.records_by_time, self.serve_orders), open(self.records_by_time_dir, "wb"))


    def step(self, action=None):
        # 每2秒钟需要做如下5个步骤
        sec = self.sec
        self.sec += 2
        reward = 0
        observation = dict()
        # 处理动作输入
        matching, repo_result = action.get('matching', None), action.get('repo', None)
        if matching is not None:
            matching_time = time.time() - self.next_begin_time
            real_match = set(candidate for candidate in matching
                                if not Simulator.reject(candidate, self.order_hash_to_info, self.avail_driver_dict))
            self.serve_orders.update(set(candidate[0] for candidate in real_match))
            driver_order = [(candidate[1], self.order_hash_to_info[candidate[0]])
                            for candidate in real_match]
            rewards = sum(self.order_hash_to_info[candidate[0]].reward for candidate in real_match)
            reward = rewards
            DriverGroup.assign_order_to_drivers(driver_order)
            self.statistics['ans_rate'] += len(matching) / self.next_orders_len
            self.statistics['comp_rate'] += len(real_match) / self.next_orders_len
            self.statistics['accu_rewards'] += rewards
            self.statistics['seg_rewards'] += rewards
            self.statistics['matching_time'] += matching_time
        if repo_result is not None:
            DriverGroup.repo_drivers(repo_result)
        if sec == self.start_time:
            online_drivers = DriverGroup.init_drivers(self.start_time)
            observation['online_drivers'] = online_drivers
            print("finish loading, begin simulate")
        
        if sec != self.start_time:
            # STEP4 更新所有司机状态
            # update
            DriverGroup.update(sec)
            # STEP5 更新统计信息和轨迹相关信息用于可视化
            self.records_by_time[sec]['driver_heat'] = []
            self.records_by_time[sec]['order_heat'] = []
            self.records_by_time[sec]['stats'] = {}
            self.records_by_time[sec]['track'] = []
            for d_hash, driver in DriverGroup.drivers.items():
                order_id, duration = driver.get_info()
                if duration != -1:
                    self.records_by_time[sec]['track'].append({'driver_id': d_hash, 'order_id': order_id, 'duration': duration})

            if sec % LOG_FREQUENCY == 0:
                self.statistics['ans_rate'] /= (LOG_FREQUENCY / 2)
                self.statistics['comp_rate'] /= (LOG_FREQUENCY / 2)
                print("%s: %5d/%5d drivers, %5d orders, r_ans: %.3f, r_comp: %.3f, match time %.3f, repo time %.3f, "
                        "rewards: %.3f, total rewards: %.3f" %
                        (sec_to_human(sec), self.statistics['avail_driver_num'], self.statistics['total_driver_num'],
                        self.statistics['order_num'], self.statistics['ans_rate'], self.statistics['comp_rate'],
                        self.statistics['matching_time'], self.statistics['repo_time'], self.statistics['seg_rewards'],
                        self.statistics['accu_rewards']))
                with open(self.processing_dir, "a") as f:
                    f.writelines(str(sec) + "\n")
                for order in self.chunk_orders:
                    self.records_by_time[sec]['order_heat'].append([order.start_lng, order.start_lat])
                self.chunk_orders.clear()

                for d_hash, driver in DriverGroup.drivers.items():
                    if driver.is_assign:
                        self.records_by_time[sec]['driver_heat'].append([*driver.get_location()])

                self.records_by_time[sec]['stats'] = copy.copy(self.statistics)
                self.statistics.update(order_num=0, ans_rate=0, comp_rate=0, seg_rewards=0)
        # STEP1 更新司机
        offline_drivers = DriverGroup.log_off(sec)
        observation = dict()
        observation['offline_drivers'] = offline_drivers
        observation['seconds'] = sec
        online_drivers = DriverGroup.log_on(sec)
        if sec != self.start_time:
            observation['online_drivers'] = online_drivers
        # STEP2 获取订单、执行分单算法
        # assignment
        orders = DataLoader.get_orders(sec)
        # 按照设定的频率更新统计信息
        if sec % LOG_FREQUENCY == 0:
            self.statistics.update(total_driver_num=DriverGroup.get_driver_numbers(),
                                    avail_driver_num=DriverGroup.get_avail_driver_numbers())
        observation['orders'] = orders
        # 这个2s有订单 则构建二分图 执行分单算法
        if orders:
            self.next_orders_len = len(orders)
            self.chunk_orders.extend(orders)
            self.statistics['order_num'] += len(orders)
            self.grid_to_drivers, self.avail_driver_dict = DriverGroup.get_assign_drivers()
            self.order_hash_to_info = {order.hashcode: order for order in orders}
            dispatch_observ = self.build_dispatch_observ(sec, orders, self.grid_to_drivers, self.avail_driver_dict)
            observation['dispatch_observ'] = dispatch_observ            
        # STEP3 执行调度算法
        repo_drivers = DriverGroup.get_repo_drivers(sec, REPO_FREQUENCY)
        if repo_drivers:
            repo_observ = self.build_repo_observ(sec, repo_drivers)
            observation['repo_observ'] = repo_observ
        self.next_begin_time = time.time()
        return observation, reward, sec == self.finish_time + 2, None
        
if __name__ == "__main__":
    simu = Simulator(Agent())
    for cur_day in range(begin_day, end_day + 1):
        simu.update_day(cur_day)
        t1 = time.process_time()
        simu.start_simulation()
        t2 = time.process_time()
        print("finish simulate day%08d in %d seconds" % (cur_day, (t2 - t1)))
