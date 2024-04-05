from typing import List, Tuple
import pickle
import os
import numpy as np
from .utils import acc_dist
from ..settings import seed, DATA_PATH

np.random.seed(seed)


class Grids:
    """
    this class can find the hex of a point
    """
    grid_adjacent, grid_centers, grid_ids, grid_infos, mesh, step_lng, step_lat, min_lng, min_lat, city_center = \
        pickle.load(open(os.path.join(DATA_PATH, "geo_info"), "rb"))
    dx = [0, 0, 0, 1, 1, 1, -1, -1, -1]
    dy = [0, 1, -1, 1, 0, -1, 0, 1, -1]

    @staticmethod
    def _find_grid_more(lng: float, lat: float, k=8) -> List[str]:
        i, j = int((lng - Grids.min_lng) / Grids.step_lng) + 1, int((lat - Grids.min_lat) / Grids.step_lat) + 1
        res = []
        try:
            for di in range(9):
                for idx in Grids.mesh[i + Grids.dx[di]][j + Grids.dy[di]]:
                    lng_lat = Grids.grid_centers[idx]
                    dis = (lng - lng_lat[0]) * (lng - lng_lat[0]) + (lat - lng_lat[1]) * (lat - lng_lat[1])
                    res.append((dis, idx))
        except IndexError:
            print("Warning: out of border!")
        res.sort()
        res = res[:min(k, len(res))]
        return [each[1] for each in res]

    @staticmethod
    def find_grid_more(grid_id, k=6):
        # 查找一个六边形网格(grid_id)周围的网格
        return Grids.grid_adjacent[grid_id][0: k]

    @staticmethod
    def find_grid_by_current(lng, lat, grid):
        lng1, lat1 = Grids.grid_centers[grid]
        if acc_dist(lng, lat, lng1, lat1) < 250:
            return grid
        rec_min = 900000000
        rec_idx = ""
        for adj in Grids.grid_adjacent[grid]:
            dis = acc_dist(lng, lat, lng1, lat1)
            if rec_min > dis:
                rec_min = dis
                rec_idx = adj
                if dis < 250:
                    return adj
        return rec_idx

    @staticmethod
    def find_grid(lng: float, lat: float) -> str:
        # 根据坐标(lng, lat)查找指定网格
        i, j = int((lng - Grids.min_lng) / Grids.step_lng) + 1, int((lat - Grids.min_lat) / Grids.step_lat) + 1
        min_dis = 900000000
        rec_idx = ""
        try:
            for di in range(9):
                for idx in Grids.mesh[i + Grids.dx[di]][j + Grids.dy[di]]:
                    dis = acc_dist(lng, lat, *Grids.grid_centers[idx])
                    # dis = (lng - lng_lat[0]) * (lng - lng_lat[0]) + (lat - lng_lat[1]) * (lat - lng_lat[1])
                    if dis < 250:
                        return idx
                    if min_dis > dis:
                        rec_idx = idx
                        min_dis = dis
        except IndexError:
            # print("Warning: out of border!")
            rec_idx = Grids.grid_ids[0]
        return rec_idx

    @staticmethod
    def gen_random(grid_id: str) -> Tuple[float, float]:
        # 从指定网格(grid_id)中随机选取一个位置
        points = Grids.grid_infos[grid_id]
        weights = np.random.random(size=6)
        denominator = np.sum(weights)
        x = (points[0][0] * weights[0] + points[1][0] * weights[1] + points[2][0] * weights[2] +
             points[3][0] * weights[3] + points[4][0] * weights[4] + points[5][0] * weights[5]) / denominator
        y = (points[0][1] * weights[0] + points[1][1] * weights[1] + points[2][1] * weights[2] +
             points[3][1] * weights[3] + points[4][1] * weights[4] + points[5][1] * weights[5]) / denominator
        return x, y

    @staticmethod
    def away_from_center(grid_id):
        # 返回指定网格(grid_id)距离城市中心的距离
        return acc_dist(*Grids.grid_centers[grid_id], *Grids.city_center)

    @staticmethod
    def get_grid_location(grid_id):
        # 返回指定网格(grid_id)的中心
        return Grids.grid_centers[grid_id]
