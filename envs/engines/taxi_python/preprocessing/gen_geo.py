import pickle
from Core.settings import DATA_PATH
from os.path import join
from scipy.spatial import KDTree
import json

# record for center, hash code and six points of each grid, respectively
grid_centers, grid_ids, grid_infos = dict(), [], dict()
center_points = []
grid_adjacent = dict()
# read and filter correct grids
f = open(join(DATA_PATH, "hexagon_grid_table.csv"), "rb")
line = f.readline().decode()
while line:
    id_points = line.strip().split(',')
    grid_id, points = id_points[0], [float(each) for each in id_points[1:]]
    if 0.5 < points[0] < 180:
        grid_ids.append(grid_id)
        grid_centers[grid_id] = [sum([points[i] for i in range(0, 12, 2)]) / 6,
                                 sum([points[i] for i in range(1, 12, 2)]) / 6]
        center_points.append(grid_centers[grid_id])
        grid_infos[grid_id] = [(points[i], points[i + 1]) for i in range(0, 12, 2)]
    line = f.readline().decode()
f.close()

# calculate adjacent
kdtree = KDTree(center_points)
for grid_id in grid_ids:
    _, indices = kdtree.query(grid_centers[grid_id], k=7)
    grid_adjacent[grid_id] = [grid_ids[idx] for idx in indices]

# calculate the step length for net index
div_granularity = 2000
center_lng, center_lat = [grid_centers[grid_id][0] for grid_id in grid_centers], \
                         [grid_centers[grid_id][1] for grid_id in grid_centers]
min_lng, min_lat, max_lng, max_lat = min(center_lng), min(center_lat), max(center_lng), max(center_lat)
step_lng, step_lat = (max_lng - min_lng) / div_granularity, (max_lat - min_lat) / div_granularity
# PARAM = [102.993580, 29.208670, 2.912220, 1.791320, 50]
print(min_lng, max_lng, min_lat, max_lat, step_lng, step_lat)

# build grid net index for grids
mesh = [[[] for i in range(div_granularity + 20)] for j in range(div_granularity + 20)]
for idx, grid_id in enumerate(grid_ids):
    lng, lat = grid_centers[grid_id]
    mesh[int((lng - min_lng) / step_lng) + 1][int((lat - min_lat) / step_lat) + 1].append(grid_id)

# check the split effectiveness
for i in range(div_granularity + 20):
    for j in range(div_granularity + 20):
        if len(mesh[i][j]) > 1:
            print(len(mesh[i][j]))

city_center = [104.067923463, 30.6799428454]
# save all data for load
pickle.dump((grid_adjacent, grid_centers, grid_ids, grid_infos, mesh, step_lng, step_lat, min_lng, min_lat, city_center),
            open(join(DATA_PATH, 'geo_info'), 'wb'))
geo_info = {
    'grid_adjacent': grid_adjacent,
    'grid_centers': grid_centers,
    'grid_ids': grid_ids,
    'grid_infos': grid_infos,
    'mesh': mesh,
    'step_lng': step_lng,
    'step_lat': step_lat,
    'min_lng': min_lng,
    'min_lat': min_lat,
    'city_center': city_center
}

json.dump(geo_info, open(join(DATA_PATH, 'geo_info.json'), 'w'))
