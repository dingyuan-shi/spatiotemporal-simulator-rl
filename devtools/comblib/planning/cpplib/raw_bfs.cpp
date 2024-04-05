#include <queue>
#include <cstring>
#include <algorithm>

#include "planning.h"
#include "raw_pre_post_processing.h"

PLANNING_RESULT cpp_bfs_planning(WEIGHTED_GRAPH g, int src, int dst) {
    int n = g.size();
    bool vis[n];
    memset(vis, false, sizeof(vis));  
    int pre[n];
    for (int i = 0; i < n; ++i) {
        pre[i] = -1;
    }
    std::queue<int> q;
    q.push(src);
    vis[src] = true;
    while (!q.empty()) {
        int cur = q.front();
        q.pop();
        for (int j = 0; j < (int)g[cur].size(); ++j) {
            int next = g[cur][j].first;
            if (vis[next]) continue;
            q.push(next);
            vis[next] = true;
            pre[next] = cur;
            if (next == dst) break;
        }
        if (pre[dst] != -1) break;
    }
    
    return get_res_from_pre(pre, dst);
}