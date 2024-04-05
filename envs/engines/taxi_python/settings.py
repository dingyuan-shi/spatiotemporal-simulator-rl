import sys
from os.path import join
import getopt
import os

# setting
DATA_PATH = join(os.environ['DSS_DIR'], "workdir/data/taxiData")

RIDE_PATH = 'total_ride_request'
TRACK_PATH = 'total_ride_track'
REC_PATH = 'records'

# 常数 不能改
SECONDS_PER_MINUTE = 60
SECONDS_PER_HOUR = 3600
SPEED = 4
IS_REPO_CAN_SERVE = True  # 被调度车辆在调度过程中能否接单
IS_TRACK = False  # 是否要加载轨迹数据

UPDATE_DRIVER_LOG_ON_OFF = 5 * SECONDS_PER_MINUTE  # 司机上下线更新的频率 目前不能改
IDLE_TRANSITION_FREQUENCY = 1 * SECONDS_PER_MINUTE  # 空车位置随机变化的频率 这个用户不能改
REPO_TRANSITION_FREQUENCY = 15  # 被调度的司机多长时间更新一次位置 这个用户不能改

# 下面的是用户可以设置的
REPO_FREQUENCY = 5 * SECONDS_PER_MINUTE  # 调度算法的执行频率 默认是5min一次
LOG_FREQUENCY = 10 * SECONDS_PER_MINUTE  # 记录统计数据频率 默认10min一次，可以输出接单率、完成率、耗时等性能统计信息

start_time = 4 * SECONDS_PER_HOUR  # 模拟的开始时间 最早就是4am
finish_time = 8 * SECONDS_PER_HOUR  # 模拟的结束时间 最晚是24am
begin_day = 20161106  # 模拟开始的日期  1～30
end_day = 20161106  # 模拟结束的日期  1～30

# parameter
seed = 0 

# if __name__ == "Core.settings":
#     opts, args = getopt.getopt(sys.argv[1:], "b:e:s:f:d:r:l:u:", )
#     for k, v in opts:
#         if k == '-b':
#             begin_day = eval(v)
#         elif k == '-e':
#             end_day = eval(v)
#         elif k == '-s':
#             start_time = eval(v)
#         elif k == '-f':
#             finish_time = eval(v)
#         elif k == '-d':
#             seed = eval(v)
#         elif k == '-r':
#             REPO_FREQUENCY = eval(v)
#         elif k == '-l':
#             LOG_FREQUENCY = eval(v)
#         elif k == '-u':
#             USER = v
