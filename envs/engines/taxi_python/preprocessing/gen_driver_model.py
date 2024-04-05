from Core.settings import DATA_PATH
from os.path import join
from collections import defaultdict
import pickle
import json

# gen log on off
# per five minutes
log_on = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
          0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1089, 24, 16, 8, 15, 11, 0, 10, 24, 13, 23, 35, 12, 15, 26, 28, 21, 33, 24, 29, 18, 42, 22, 41, 48, 40, 54,
          41, 59, 63, 65, 54, 61, 97, 89, 110, 179, 163, 203, 210, 219, 268, 459, 357, 390, 375, 360, 390, 362, 354,
          380, 366, 402, 307, 351, 329, 337, 342, 330, 345, 340, 343, 331, 299, 309, 263, 284, 252, 272, 247, 172, 257,
          278, 277, 307, 228, 175, 192, 150, 196, 130, 160, 76, 59, 69, 2, 39, 23, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
          0, 0, 0, 55, 151, 159, 227, 226, 202, 275, 312, 319, 313, 271, 307, 301, 224, 261, 176, 238, 149, 154,
          151, 190, 183, 154, 83, 146, 106, 116, 87, 83, 28, 52, 66, 50, 13, 47, 63, 75, 64, 0, 0, 49, 14, 0, 4, 23, 0,
          45, 0, 0, 2, 3, 0, 0, 225, 12, 17, 0, 0, 28, 0, 4, 0, 0, 0, 0, 0, 31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
          0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
          0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]

log_off = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
           0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -33, -25, -24, -29, -28, -18, -20, -24, -21, -15, -14, -17, -17, -21,
           -16, -18, -14, -8, -24, -13, -11, -27, -14, -23, -20, -15, -22, -17, -11, -16, -12, -17, -15, -17, -12, -16,
           -11, -17, -18, -16, -33, -23, -26, -26, -47, -56, -52, -77, -100, -79, -75, -71, -99, -106, -105, -96, -111,
           -102, -117, -115, -117, -119, -124, -107, -116, -122, -147, -115, -111, -106, -123, -131, -117, -109, -119,
           -139, -142, -131, -131, -141, -160, -118, -191, -227, -219, -179, -180, -178, -184, -141, -126, -149, -139,
           -98, -81, -101, -70, -70, -73, -84, -67, -80, -64, -70, -63, -48, -64, -55, -75, -52, -64, -71, -76, -70,
           -65, -67, -90, -120, -117, -141, -114, -110, -130, -128, -125, -99, -131, -117, -100, -117, -98, -109, -117,
           -110, -99, -113, -86, -115, -99, -97, -138, -126, -118, -116, -95, -106, -92, -98, -106, -75, -95, -71, -89,
           -70, -93, -114, -85, -63, -64, -71, -71, -89, -88, -83, -93, -119, -110, -108, -99, -95, -80, -84, -78, -44,
           -54, -63, -65, -59, -67, -73, -61, -49, -52, -59, -59, -65, -60, -70, -77, -63, -82, -47, -84, -85, -82,
           -87, -86, -98, -120, -93, -128, -100, -142, -119, -111, -103, -108, -106, -137, -127, -144, -134, -105,
           -120, -130, -130, -76, -87, -117, -89, -71, -103, -102, -92, -80, -64, -91, -3290, 0]

print(len(log_on), len(log_off))
denominator = -sum(log_off)
driver_log_off_distribution = [-each / denominator for each in log_off]

# transition probability
f = open(join(DATA_PATH, "idle_transition_probability"), "rb")
trans_prob = [defaultdict(list) for i in range(24)]
line = f.readline().decode()
while line:
    hour, from_grid, to_grid, prob = line.split(',')
    hour, prob = int(hour), float(prob)
    trans_prob[hour][from_grid].append((prob, to_grid))
    line = f.readline().decode()
f.close()

for hour in range(0, 24):
    for grid in trans_prob[hour]:
        trans_prob[hour][grid].sort()
driver_model = {
    'log_off': driver_log_off_distribution,
    'log_on': log_on,
    'trans_prob': trans_prob
}

pickle.dump(driver_model, open(join(DATA_PATH, "driver_model"), "wb"))
json.dump(driver_model, open(join(DATA_PATH, "driver_model.json"), "w"))