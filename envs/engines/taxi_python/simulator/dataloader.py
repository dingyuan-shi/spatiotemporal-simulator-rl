from os.path import join
from .entity import Order, Driver
from .utils import convert_to_timestamp
import pickle
from ..settings import UPDATE_DRIVER_LOG_ON_OFF, DATA_PATH, IS_TRACK
from .grids import Grids
import numpy as np

CANCEL_PATH = 'total_order_cancellation_probability'
RIDE_PATH = 'total_ride_request'
TRACK_PATH = 'total_ride_track'


class DataLoader:

    cur_order_pos, day, begin_time, rides_info, track_info, distribution = 0, 0, 0, None, None, None
    grid_coefficients = [max(0, 0.4 - (Grids.away_from_center(grid_id) // 2000) * 0.1) for grid_id in Grids.grid_ids]
    norm_denom = sum(grid_coefficients)
    grid_coefficients_norm = []
    for each in grid_coefficients:
        grid_coefficients_norm.append(each / norm_denom)
    grid_coefficients_norm = np.array(grid_coefficients_norm)

    @staticmethod
    def skip_to(begin_time):
        # 直接跳到设置的模拟开始时间begin_time
        DataLoader.begin_time = begin_time
        begin_timestamp = convert_to_timestamp(DataLoader.day, begin_time)
        new_pos = DataLoader.cur_order_pos
        while new_pos < DataLoader.rides_info.shape[0] and DataLoader.rides_info[new_pos][1] < begin_timestamp:
            new_pos += 1
        DataLoader.cur_order_pos = new_pos

    @staticmethod
    def load_data(day):
        # 加载指定日期（day）的数据
        DataLoader.day, DataLoader.cur_order_pos = day, 0
        # load order info, timestamp, location, rejection
        DataLoader.rides_info = pickle.load(open(join(DATA_PATH, RIDE_PATH, "order_with_cancel" + str(day)), "rb"))
        # load track info of rides
        if IS_TRACK:
            DataLoader.track_info = pickle.load(open(join(DATA_PATH, TRACK_PATH, "order_points_%8d" % day), 'rb'))
        # load distribution info of drivers
        driver_model = pickle.load(open(join(DATA_PATH, 'driver_model'), 'rb'))
        DataLoader.distribution = driver_model['log_on']

    @staticmethod
    def get_orders(cur_time):
        # 返回当前时刻（cur_time）的订单数据
        cur_timestamp = convert_to_timestamp(DataLoader.day, cur_time)
        new_pos = DataLoader.cur_order_pos
        batch_orders = []
        while new_pos < DataLoader.rides_info.shape[0] and DataLoader.rides_info[new_pos][1] <= cur_timestamp:
            hashcode, start_timestamp, finish_timestamp, start_lng, start_lat, finish_lng, finish_lat, reward \
                = DataLoader.rides_info[new_pos][0:8]
            if start_lng * start_lat * finish_lng * finish_lat < 0.5 or finish_timestamp - start_timestamp < 60:
                new_pos += 1
                continue
            cancel_prob = DataLoader.rides_info[new_pos][8:]
            start_grid = Grids.find_grid(start_lng, start_lat)
            if IS_TRACK:
                track = DataLoader.track_info[hashcode]
            else:
                track = None
            batch_orders.append(Order(hashcode, start_lng, start_lat, finish_lng, finish_lat, start_timestamp,
                                finish_timestamp, start_grid, reward, cancel_prob, track))
            new_pos += 1
        DataLoader.cur_order_pos = new_pos
        return batch_orders

    @staticmethod
    def get_drivers(cur_time, current_drivers=None):
        # 返回当前时刻（cur_time）上线的司机
        new_drivers = []
        total_target_number = DataLoader.distribution[cur_time // UPDATE_DRIVER_LOG_ON_OFF]
        grid_target_numbers = DataLoader.grid_coefficients_norm * total_target_number
        for idx, grid_id in enumerate(Grids.grid_ids):
            lng_lats = [Grids.gen_random(grid_id) for _ in range(int(grid_target_numbers[idx]))]
            new_drivers.extend([Driver(cur_time, lng_lats[n][0], lng_lats[n][1], grid_id, True)
                                for n in range(int(grid_target_numbers[idx]))])
        return new_drivers


if __name__ == '__main__':
    DataLoader.load_data(1)

    for i in range(1, 1000):
        print("TIME: %d" % i)
        # dy_tester orders
        orders = DataLoader.get_orders(i)
        for order in orders:
            print(order)
        # dy_tester drivers
        drivers = DataLoader.get_drivers(i)
        for driver in drivers:
            print(driver)
