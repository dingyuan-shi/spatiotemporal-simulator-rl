from typing import Set, Dict, Any
from collections import namedtuple
from .recorder import Recorder
import pickle
import random
random.seed(0)
import os
ODPair = namedtuple('ODPair', ['order_id', 'driver_id'])
DDPair = namedtuple('DGPair', ['driver_id', 'destination'])


class Agent(Recorder):
    """ Agent for dispatching and reposition """

    def __init__(self, **kwargs):
        """ Load your trained model and initialize the parameters """
        super().__init__()
        self.grids = pickle.load(open(os.path.join(os.path.dirname(__file__), "grid_ids"), "rb"))

    def dispatch(self, dispatch_observ) -> Set[Dict[str, int]]:
        """ Compute the assignment between drivers and passengers at each time step
        :param dispatch_observ: a set of namedtuple, the name in the namedtuple includes:
                order_id, int
                driver_id, int
                order_driver_distance, float
                order_start_location, a list as [lng, lat], float
                order_finish_location, a list as [lng, lat], float
                driver_location, a list as [lng, lat], float
                timestamp, int
                order_finish_timestamp, int
                day_of_week, int
                reward_units, float
                pick_up_eta, float
        :return: a set of namedtuple, the name in the dict includes:
                order_id and driver_id, the pair indicating the assignment
        """
        dispatch_observ = list(dispatch_observ)
        dispatch_observ.sort(key=lambda od_info: od_info.order_driver_distance)
        assigned_order = set()
        assigned_driver = set()
        dispatch_action = []
        for od in dispatch_observ:
            # make sure each order is assigned to one driver, and each driver is assigned with one order
            if (od.order_id in assigned_order) or (od.driver_id in assigned_driver):
                continue
            assigned_order.add(od.order_id)
            assigned_driver.add(od.driver_id)
            dispatch_action.append(ODPair(order_id=od.order_id, driver_id=od.driver_id))
        return set(dispatch_action)

    def reposition(self, repo_observ):
        """ Compute the reposition action for the given drivers
        :param repo_observ: a dict, the key in the dict includes:
                timestamp: int
                driver_info: a set of namedtuple, the name in the tuple includes:
                        driver_id: driver_id of the idle driver in the treatment group, int
                        grid_id: id of the grid the driver is located at, str
                day_of_week: int
        :return: a set of namedtuple, the name in the tuple includes:
                driver_id: corresponding to the driver_id in the od_list
                destination: id of the grid the driver is repositioned to, str
        """
        repo_action = set()
        for driver in repo_observ.driver_info:
            # random select a grid
            grid_id = random.choice(self.grids)
            repo_action.add(DDPair(driver_id=driver.driver_id, destination=grid_id))
        return repo_action
