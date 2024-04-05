#include <vector>
#include <cstring>
#include <cmath>
#include "matching.h"
#include "raw_pre_post_processing.h"

#define INF 100000000
#define zero_threshold 0.0000001

bool _dfs(int u, int m, double * rslack, int * lmatch, int * rmatch, bool * lvis, bool * rvis, const double * lexception, const double * rexception, const double * weight_matrix_flatten) {
    lvis[u] = true;
    for (int v = 0; v < m; ++v) {
        if (rvis[v]) continue;
        double t = lexception[u] + rexception[v] - weight_matrix_flatten[u * m + v];
        if (fabs(t) < zero_threshold) {
            rvis[v] = true;
            if (rmatch[v] == -1 || _dfs(rmatch[v], m, rslack, lmatch, rmatch, lvis, rvis, lexception, rexception, weight_matrix_flatten)) {
                lmatch[u] = v;
                rmatch[v] = u;
                return true;
            }
        } else if (rslack[v] - t > zero_threshold) {
            rslack[v] = t;
        }
    }
    return false;
}
MATCHING_RESULT cpp_kuhn_munkres_matching(std::vector<WEIGHTED_EDGE> edges, bool is_max) {
    bool is_switch = false;
    std::unordered_map<int, int> left_name_to_idx;
    std::unordered_map<int, int> right_name_to_idx;
    std::vector<int> left_idx_to_name;
    std::vector<int> right_idx_to_name;
    
    _mapping_nodes_weighted(edges, is_switch, is_max, left_name_to_idx, right_name_to_idx, left_idx_to_name, right_idx_to_name);
    int n = left_idx_to_name.size();
    int m = right_idx_to_name.size();
    std::vector<std::pair<int, double> > adj[n];
    _build_graph_weighted(edges, is_switch, left_name_to_idx, right_name_to_idx, adj);
    double weight_matrix_flatten[n * m];
    memset(weight_matrix_flatten, 0, sizeof(weight_matrix_flatten));
    for (int i = 0; i < n; ++i) {
        for (int j = 0; j < (int)adj[i].size(); ++j) {
            weight_matrix_flatten[i * m + adj[i][j].first] = adj[i][j].second;
        }
    }
    bool lvis[n];
    bool rvis[m];
    int lmatch[n];
    int rmatch[m];
    for (int i = 0; i < n; ++i) {
        lmatch[i] = -1;
    }
    for (int j = 0; j < m; ++j) {
        rmatch[j] = -1;
    }
    
    double rslack[m];
    double rexception[m];
    double lexception[n];
    memset(rexception, 0, sizeof(rexception));
    memset(lexception, 0, sizeof(rexception));

    for (int i = 0; i < n; ++i) {
        for (int j = 0; j < (int)adj[i].size(); ++j) {
            lexception[i] = std::max(lexception[i], adj[i][j].second);
        }
    }

    for (int i = 0; i < n; ++i) {
        for (int j = 0; j < m; ++j) {
            rslack[j] = INF;
        }
        while (true) {
            // memset(rvis, false, sizeof(rvis));
            // memset(lvis, false, sizeof(lvis));
            for (int k = 0; k < n; ++k) {
                lvis[k] = false;
            }
            for (int k = 0; k < m; ++k) {
                rvis[k] = false;
            }
            if (_dfs(i, m, rslack, lmatch, rmatch, lvis, rvis, lexception, rexception, weight_matrix_flatten)) break;
            double d = INF;
            for (int j = 0; j < m; ++j) {
                if (!rvis[j] && d > rslack[j]) {
                    d = rslack[j];
                } 
            }
            if (fabs(d - INF) < zero_threshold || d < zero_threshold) break;
            for (int k = 0; k < n; ++k) {
                if (lvis[k]) {
                    lexception[k] -=d;
                }
            }
            for (int j = 0; j < m; ++j) {
                if (rvis[j]) {
                    rexception[j] += d;
                } else {
                    rslack[j] -= d;
                }
            }
        }
    }

    MATCHING_RESULT res;
    _convert_back(res, lmatch, is_switch, left_idx_to_name, right_idx_to_name);
    return res;
}