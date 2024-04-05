from .dataloader import DataLoader
from ..settings import UPDATE_DRIVER_LOG_ON_OFF, IS_REPO_CAN_SERVE
from collections import defaultdict
from .entity import Driver
# from entity_driver import Driver
# SERVE = Driver.State.SERVE


class DriverGroup:
    drivers = dict()
    init_time = 0
    avail = 0

    @staticmethod
    def init_drivers(cur_time):
        DriverGroup.init_time = cur_time
        init_drivers = DataLoader.get_drivers(cur_time)
        for driver in init_drivers:
            DriverGroup.drivers[driver.hashcode] = driver
        return set(driver.hashcode for driver in init_drivers)

    @staticmethod
    def log_off(cur_time):
        if cur_time == DriverGroup.init_time or cur_time % UPDATE_DRIVER_LOG_ON_OFF != 0:
            return set()
        offlines = set()
        for driver in list(DriverGroup.drivers.values()):
            if driver.log_off(cur_time // UPDATE_DRIVER_LOG_ON_OFF):
                del DriverGroup.drivers[driver.hashcode]
                offlines.add(driver.hashcode)
        return offlines

    @staticmethod
    def log_on(cur_time):
        if cur_time == DriverGroup.init_time or cur_time % UPDATE_DRIVER_LOG_ON_OFF != 0:
            return set()
        new_drivers = DataLoader.get_drivers(cur_time)
        for driver in new_drivers:
            DriverGroup.drivers[driver.hashcode] = driver
        return set(driver.hashcode for driver in new_drivers)

    @staticmethod
    def get_assign_drivers():
        avail_driver_dict = {d_hash: driver for d_hash, driver in DriverGroup.drivers.items() if driver.is_assign}
        grid_to_drivers = defaultdict(set)
        for d_hash, driver in avail_driver_dict.items():
            grid_to_drivers[driver.grid].add(d_hash)
        DriverGroup.avail = len(avail_driver_dict)
        return grid_to_drivers, avail_driver_dict

    @staticmethod
    def assign_order_to_drivers(driver_order):
        for hashcode, order in driver_order:
            driver = DriverGroup.drivers[hashcode]
            driver.assign_order(order)

    @staticmethod
    def get_repo_drivers(cur_time, repo_freq):
        if cur_time % repo_freq != 0:
            return []
        return [(d.lng, d.lat, d.grid, d_hash)
                for d_hash, d in DriverGroup.drivers.items() if d.can_be_repositioned(repo_freq)]

    @staticmethod
    def repo_drivers(driver_grids):
        for hashcode, grid in driver_grids:
            DriverGroup.drivers[hashcode].repo_to(grid)

    @staticmethod
    def update(cur_sec):
        for d_hash, driver in DriverGroup.drivers.items():
            driver.update(cur_sec)

    @staticmethod
    def get_driver_numbers():
        return len(DriverGroup.drivers)

    @staticmethod
    def get_avail_driver_numbers():
        return DriverGroup.avail

    @staticmethod
    def get_serve_drivers():
        return {d_hash: driver for d_hash, driver in DriverGroup.drivers.items() if not driver.is_assign}

    @staticmethod
    def clear():
        DriverGroup.drivers.clear()
