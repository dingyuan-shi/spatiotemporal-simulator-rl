#ifndef _PLANNING_H_
#define _PLANNING_H_

#include <vector>

typedef std::vector<std::vector<std::pair<int, double> > > WEIGHTED_GRAPH;
typedef std::vector<int> PLANNING_RESULT;

PLANNING_RESULT cpp_bfs_planning(WEIGHTED_GRAPH g, int src, int dst);
PLANNING_RESULT cpp_dfs_planning(WEIGHTED_GRAPH g, int src, int dst);
PLANNING_RESULT cpp_dijkstra_planning(WEIGHTED_GRAPH g, int src, int dst);

#endif