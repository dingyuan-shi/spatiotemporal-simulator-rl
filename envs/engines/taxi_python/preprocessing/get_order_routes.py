import requests
import csv
import pickle
import traceback
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
# order file
dataPath = r"D:\工作\simulator\simulator\data\SimulatorData\SimulatorData\total_ride_request\order_20161106.csv"
# output file
targetPath = r"D:\工作\simulator\SimulatorData\SimulatorData\total_ride_request\order_lack.txt"


class Order:
    def __init__(self, id, stime, etime, slng, slat, elng, elat, route=None):
        self.id = id
        self.stime = stime
        self.etime = etime
        self.slng = slng
        self.slat = slat
        self.elng = elng
        self.elat = elat
        self.route = route


# baidu api should be lat,lng instead of lng,lat
def getRoutes(base_url, start, end, accessToken):
    url = base_url + "origin=" + str(start[1]) + ',' + str(start[0]) + '&' \
          + "destination=" + str(end[1]) + ',' + str(end[0]) + '&ak=' + accessToken
    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.182 Safari/537.36'}
    rep = requests.get(url, headers=headers)
    route = []
    route.extend([float(start[0]), float(start[1])])
    rep = rep.json()
    for step in rep['result']['routes'][0]['steps']:
        route.append(float(step['end_location']['lng']))
        route.append(float(step['end_location']['lat']))
    return route


def toPickle(dataPath, targetPath):
    orderList = []
    count = 0
    startindex = 0
    with open(dataPath, 'r') as f:
        reader = csv.reader(f)

        for item in reader:
            start = [item[3], item[4]]
            end = [item[5], item[6]]
            # route = getRoutes(start, end)
            myOrder = Order(item[0], item[1], item[2], item[3], item[4], item[5], item[6])
            orderList.append(myOrder)

    with open(targetPath, 'wb') as f2:
        pickle.dump(orderList, f2)


def toRequiredForm(orderList):
    required = {}
    for item in orderList:
        required[item.id] = item.route
    return required


def searchRoute(targetPath):
    with open(targetPath, 'rb') as f:
        orderList = pickle.load(f)
    # set initial parameters

    count = 0
    startIndex = 0
    accessTokensIndex = 0
    baseURLIndex = 0

    while True:
        try:
            while startIndex < len(orderList):
                item = orderList[startIndex]
                if item.route == None:
                    start = [item.slng, item.slat]
                    end = [item.elng, item.elat]

                    route = getRoutes(base_url[baseURLIndex], start, end, accessTokens[accessTokensIndex])
                    orderList[startIndex].route = route
                    count += 1
                if count == 1000:
                    with open(targetPath, 'wb') as f:
                        pickle.dump(toRequiredForm(orderList), f)
                    count = 0
                startIndex += 1
        except Exception as err:
            traceback.print_exc()
            if accessTokensIndex < len(accessTokens) - 1:
                accessTokensIndex += 1
                continue
            else:
                if baseURLIndex < len(base_url) - 1:
                    baseURLIndex += 1
                    accessTokensIndex = 0
                    continue
                else:
                    print('interrupted: now index is %d' % startIndex)
                    break
        finally:
            with open(targetPath, 'wb') as f:
                pickle.dump(toRequiredForm(orderList), f)
            print('base_url_index:' + str(baseURLIndex))
            print('start_index:' + str(startIndex))
            print('accessTokenIndex:' + str(accessTokensIndex))
            if baseURLIndex >= len(base_url) - 1 and accessTokensIndex >= len(accessTokens) - 1:
                break


if __name__ == '__main__':

    # only for the first time
    if not os.path.exists(targetPath):
        toPickle(dataPath, targetPath)

    searchRoute(targetPath)

    # with open(targetPath,'rb') as f:
    #     orderList = pickle.load(f)
    # tmp = orderList[-1]
    # with open(r"D:\工作\simulator\SimulatorData\SimulatorData\total_ride_request\order",'rb') as f:
    #     res =  pickle.load(f)
    # pass
    # print(tmp.__dict__)

    # lack_id = []
    # with open(targetPath,'r') as f:
    #     for line in f:
    #         id = line.strip('\n').split(' ')[0]
    #         lack_id.append(id)
    # orderList = {}
    # res = {}
    # with open(r"D:\工作\simulator\SimulatorData\SimulatorData\total_ride_request\order_20161106.csv",'r') as f:
    #     reader = csv.reader(f)
    #     for item in reader:
    #         start = [item[3], item[4]]
    #         end = [item[5], item[6]]
    #         # route = getRoutes(start, end)
    #         orderList[item[0]] = [item[3],item[4],item[5],item[6]]
    #     for id in lack_id:
    #         start = [orderList[id][0],orderList[id][1]]
    #         end = [orderList[id][2],orderList[id][3]]
    #         route = getRoutes(base_url[0],start,end,accessTokens[5])
    #         res[id] = route
    # with open(r"D:\工作\simulator\SimulatorData\SimulatorData\total_ride_request\order",'wb') as f:
    #     pickle.dump(res,f)
    #
    #
    # pass
