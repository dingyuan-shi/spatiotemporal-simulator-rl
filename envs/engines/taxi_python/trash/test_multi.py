from time import process_time
# from entity_order import Order
from Core.simulator.entity import Order
from entity_driver import Driver
import numpy as np
import time

if __name__ == "__main__":
    p1 = time.process_time()
    for i in range(100000):
        order = Order("aaa", 101.1, 30.4, 102.5, 10.9, 1400000, 1500000, "bbb", 22.7, np.zeros(5), np.zeros((10, 2)))
    p2 = time.process_time()
    print(p2 - p1)
    # driver = Driver(14400, 104.1, 103.2, "122", True)

