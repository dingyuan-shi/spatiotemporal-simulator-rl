#include <cstring>
#include <algorithm>

#include "planning.h"
#include "raw_pre_post_processing.h"

void _dfs(int cur, const WEIGHTED_GRAPH& g, int dst, bool * vis, int * pre) {
    vis[cur] = true;
    for (int j = 0; j < (int)g[cur].size(); ++j) {
        int next = g[cur][j].first;
        if (vis[next]) continue;
        pre[next] = cur;
        if (next == dst) break;
        _dfs(next, g, dst, vis, pre);
    }
    return;
}

PLANNING_RESULT cpp_dfs_planning(WEIGHTED_GRAPH g, int src, int dst) {
    int n = g.size();
    bool vis[n];
    memset(vis, false, sizeof(vis));
    int pre[n];
    for (int i = 0; i < n; ++i) pre[i] = -1;
    _dfs(src, g, dst, vis, pre);

    return get_res_from_pre(pre, dst);
}