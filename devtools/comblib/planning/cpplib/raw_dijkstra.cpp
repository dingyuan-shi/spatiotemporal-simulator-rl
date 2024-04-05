#include <cstring>
#include <queue>
#include <algorithm>
#include <iostream>

#include "planning.h"
#include "raw_pre_post_processing.h"

struct cmp{
   bool operator()(std::pair<double, int>& a, std::pair<double, int>& b){
       return a.second > b.second; 
   }
};

PLANNING_RESULT cpp_dijkstra_planning(WEIGHTED_GRAPH g, int src, int dst) {
    int n = g.size();
    bool vis[n];
    memset(vis, false, sizeof(vis));
    int pre[n];
    double dist[n];
    for (int i = 0; i < n; ++i) {
        dist[i] = -1;
        pre[i] = -1;
    }
    dist[src] = 0;
    std::priority_queue<std::pair<double, int>, std::vector<std::pair<double, int> >, cmp> q;
    q.push(std::make_pair(src, 0));
    while (!q.empty()) {
        std::pair<double, int> dist_cur = q.top();
        q.pop();
        int cur = dist_cur.first;
        if (vis[cur]) continue;
        vis[cur] = true;
        for (int j = 0; j < (int)g[cur].size(); ++j) {
            int next = g[cur][j].first;
            double weight = g[cur][j].second;
            if (dist[next] == -1 || dist[next] > dist[cur] + weight) {
                dist[next] = dist[cur] + weight;
                pre[next] = cur;
                q.push(std::make_pair(next, dist[next]));
            }
            
        }
    }
    return get_res_from_pre(pre, dst);
}