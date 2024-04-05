#include <vector>
#include <unordered_map>
#include <cstring>
#include <iostream>
#include "matching.h"
#include "raw_pre_post_processing.h"


bool _dfs(int u, const std::vector<int>* adj, bool* vis, int* lmatch, int* rmatch){
    for(int i = 0; i < (int)adj[u].size(); ++i){
        int v= adj[u][i];
        if(vis[v])  
            continue;
        vis[v] = true;
        if(rmatch[v] == -1 || _dfs(rmatch[v], adj, vis, lmatch, rmatch)){
            rmatch[v]=u;
            lmatch[u] = v;
            return true;
        }
    }
    return false;
}




MATCHING_RESULT cpp_hungary_matching(std::vector<UNWEIGHTED_EDGE> edges) {
    bool is_switch = false;
    std::unordered_map<int, int> left_name_to_idx;
    std::unordered_map<int, int> right_name_to_idx;
    std::vector<int> left_idx_to_name;
    std::vector<int> right_idx_to_name;
    
    _mapping_nodes_unweighted(edges, is_switch, left_name_to_idx, right_name_to_idx, left_idx_to_name, right_idx_to_name);
    int n = left_idx_to_name.size();
    int m = right_idx_to_name.size();
    std::vector<int> adj[n];
    _build_graph_unweighted(edges, is_switch, left_name_to_idx, right_name_to_idx, adj);
    
    bool vis[m];
    int lmatch[n];
    int rmatch[m];
    for (int i = 0; i < n; ++i) {
        lmatch[i] = -1;
    }
    for (int i = 0; i < m; ++i) {
        rmatch[i] = -1;
    }

    for (int i = 0; i < n; ++i) {
        memset(vis, false, sizeof(vis));
        if (lmatch[i] == -1) {
            _dfs(i, adj, vis, lmatch, rmatch);
        }
    }

    // convert back
    MATCHING_RESULT res;
    _convert_back(res, lmatch, is_switch, left_idx_to_name, right_idx_to_name);
    return res;
}
