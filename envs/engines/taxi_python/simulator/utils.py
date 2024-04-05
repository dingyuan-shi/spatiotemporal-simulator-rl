import time
from math import asin, sin, cos, sqrt
import random
import pickle
from os.path import join
from ..settings import DATA_PATH, seed
from numba import jit

random.seed(seed)

trans_prob = pickle.load(open(join(DATA_PATH, "driver_model"), "rb"))['trans_prob']


def convert_to_timestamp(day, day_seconds):
    hour, minute, second = day_seconds // 3600, (day_seconds % 3600) // 60, day_seconds % 60
    return int(time.mktime(time.struct_time([2016, 11, day % 20161100, hour, minute, second, 0, 0, 0])))


@jit(nopython=True)
def acc_dist(lng1, lat1, lng2, lat2):
    delta_lat = (lat1 - lat2) / 2
    delta_lng = (lng1 - lng2) / 2
    arc_pi = 3.14159265359 / 180
    return 2 * 6378137 * asin(
        sqrt(sin(arc_pi * delta_lat) ** 2 + cos(arc_pi * lat1) * cos(arc_pi * lat2) * (sin(arc_pi * delta_lng) ** 2)))


def sec_to_human(sec):
    return "%02d:%02d" % (sec // 3600, (sec % 3600) // 60)


def transition(cur_sec, src_grid):
    r = random.random(src_grid)
    for prob, grid in trans_prob[cur_sec // 3600][src_grid]:
        r -= prob
        if r < 0:
            return grid
    return src_grid
