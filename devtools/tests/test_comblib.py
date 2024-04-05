from comblib.matching import greedy, hungary, kuhn_munkres
from comblib.planning import bfs, dfs, dijkstra
import random
import time
import numpy as np

edges = [
    ((0, 1), 3), 
    ((3, 0), 1), 
    ((1, 2), 2), 
    ((2, 1), 1), 
    ((3, 1), 5)
]

edges_py = [
    (0, 1, 3), 
    (3, 0, 1), 
    (1, 2, 2), 
    (2, 1, 1),
    (3, 1, 5),
]


unweighted_edges = [
    (0, 1), 
    (3, 0), 
    (1, 2), 
    (2, 1), 
]

print("test greedy matching...")
res = greedy(edges, False)
print(res)

print("test Hungary matching...")
res = hungary(unweighted_edges)
print(res)

def get_max_value_result(matrix, pairs):
    ret = 0
    for pair in pairs:
        ret += matrix[pair[0]][pair[1]]
    return ret

values_py = []
values_cpp = []
n = 1000
m = 1000
matrix = np.zeros(shape=[n, m])
random.seed(0)
for i in range(n):
    for j in range(m):
        if i // 100 == j // 1000:
            value = random.random() * 10 + 1
            values_py.append((i, j, value))
            values_cpp.append(((i, j), value))
            matrix[i][j] = value
            
print("test KM matching...")
s_time = time.time()
match_cpp = kuhn_munkres(values_cpp)
print("time usage: %s " % str(time.time() - s_time))

match_cpp_tuples = []
for key in match_cpp:
    match_cpp_tuples.append((key, match_cpp[key]))
    match_cpp_tuples.sort()

res_cpp = get_max_value_result(matrix, match_cpp_tuples)

print(res_cpp)



g = [
    [(4, 1), (3, 2)], 
    [(3, 1), (2, 3)], 
    [(1, 3), (3, 5), (4, 2)], 
    [(0, 2), (1, 1), (2, 5)], 
    [(0, 1), (2, 2)]
]

src = 1
dst = 4

print("test BFS...")
print(bfs(g, src, dst))
print("test DFS...")
print(dfs(g, src, dst))
print("test Dijkstra's algortihm...")
print(dijkstra(g, src, dst))