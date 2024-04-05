import pickle
import requests
import traceback
import csv
import json
import time
import os

# baidu
base_url = ["http://api.map.baidu.com/directionlite/v1/driving?",
            "http://api.map.baidu.com/direction/v2/driving?"]
accessTokens = ["RRSRoUIY1YZZ2S7HmShwjB7MGt7GdtLG",
                "zGxU4Wu5TADmtDCQzvqFBHye6Iiekoj9",
                "WdBdRDdW9QxI5CHv1eh4mRsUeiaB3ZaZ",
                "u3NEE53Gm4D0x1S90Ak4rMeFnR8bavOc",
                "HbUDx2cPzhizxyKKGg14BkuvABV6oFBu",
                "hh8NpCMkEAW6juD13mQLUFE1j4gmn7sV",
                "aNYbGLwWpMsUL8Pakh6T5vDln61PsSIx"]
gridCentersPath = '../data/grid_centers'
routePath = '../data/routes'


# baidu api should be lat,lng instead of lng,lat
def getRoutes(base_url, start, end, accessToken):
    url = base_url + "origin=" + str(start[1]) + ',' + str(start[0]) + '&' \
          + "destination=" + str(end[1]) + ',' + str(end[0]) + '&ak=' + accessToken
    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.182 Safari/537.36'}
    rep = requests.get(url, headers=headers)
    route = []
    route.append([float(start[0]), float(start[1])])
    rep = rep.json()
    for step in rep['result']['routes'][0]['steps']:
        route.append([float(step['end_location']['lng']), float(step['end_location']['lat'])])
    return route







if __name__ == '__main__':


    grid = pickle.load(open(gridCentersPath, 'rb'))

    # select 1000 centers closest to the given center
    center = [104.067923463, 30.6799428454]
    sorted_grid = sorted(grid.items(), key=lambda x: (x[1][0] - center[0]) ** 2 + (x[1][1] - center[1]) ** 2)
    del sorted_grid[1000:]


    # initial parameter
    # start from
    startIndex = [0, 1]
    accessTokenIndex = 0
    base_url_index = 0

    # make sure there is a file to be loaded
    if not os.path.exists(routePath):
        with open(routePath,'wb') as f:
            pickle.dump({},f)

    with open(routePath, 'rb') as f:
        routes = pickle.load(f)

    # after count times search , the result will be saved
    count = 0

    while True:
        try:
            while startIndex[0] < len(sorted_grid):
                while startIndex[1] < len(sorted_grid):
                    start = sorted_grid[startIndex[0]][1]
                    end = sorted_grid[startIndex[1]][1]
                    if ((sorted_grid[startIndex[0]][0], sorted_grid[startIndex[1]][0]) not in routes.keys()) \
                            or ((sorted_grid[startIndex[1]][0], sorted_grid[startIndex[0]][0]) not in routes.keys()):
                        route = getRoutes(base_url[base_url_index], start, end, accessTokens[accessTokenIndex])
                        routes[(sorted_grid[startIndex[0]][0], sorted_grid[startIndex[1]][0])] = route
                        route.reverse()
                        routes[(sorted_grid[startIndex[1]][0], sorted_grid[startIndex[0]][0])] = route
                    startIndex[1] = startIndex[1] + 1


                    count += 1
                    if count == 1000:
                        with open(routePath, 'wb') as f:
                            pickle.dump(routes, f)
                        count = 0
                startIndex[0] = startIndex[0] + 1
                startIndex[1] = startIndex[0] + 1

        except Exception as err:
            traceback.print_exc()
            if accessTokenIndex < len(accessTokens) - 1:
                accessTokenIndex += 1
                continue
            else:
                if base_url_index < len(base_url) - 1:
                    base_url_index += 1
                    accessTokenIndex = 0
                    continue
                else:
                    print('interrupted: now index is %d,%d' % (startIndex[0], startIndex[1]))
                    break
        finally:
            with open(routePath, 'wb') as f:
                pickle.dump(routes, f)
            print('now routes has %d items' % len(routes))
            print('startIndex: %d,%d' % (startIndex[0], startIndex[1]))
            print('base_url_index: %d' % base_url_index)
            print('accessToken is %d' % accessTokenIndex)
            if base_url_index >= len(base_url) -1 and accessTokenIndex >= len(accessTokens) -1:
                break
