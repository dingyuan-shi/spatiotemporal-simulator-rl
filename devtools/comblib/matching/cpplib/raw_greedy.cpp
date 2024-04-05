#include <vector>
#include <algorithm>
#include <unordered_map>
#include <unordered_set>
#include "matching.h"

bool _min_cmp(WEIGHTED_EDGE x, WEIGHTED_EDGE y) {
    // used for sort edges by weight ascendingly.
    return x.second < y.second;
}

bool _max_cmp(WEIGHTED_EDGE x, WEIGHTED_EDGE y) {
    // used for sort edges by weight descendingly.
	return x.second > y.second;
}

MATCHING_RESULT cpp_greedy_matching(std::vector<WEIGHTED_EDGE> edges, bool is_max) {
    MATCHING_RESULT res;
    std::sort(edges.begin(), edges.end(), is_max?_max_cmp: _min_cmp);
    std::unordered_set<int> assigned_left;
    std::unordered_set<int> assigned_right;
    // choose edges greedily based on weight
    for (std::vector<WEIGHTED_EDGE>::iterator edge = edges.begin(); edge != edges.end(); ++edge) {
        std::pair<int, int> src_dst = edge->first;
        if (assigned_left.find(src_dst.first) == assigned_left.end() && assigned_right.find(src_dst.second) == assigned_right.end()) {
            assigned_left.insert(src_dst.first);
            assigned_right.insert(src_dst.second);
            res[src_dst.first] = src_dst.second;
        }
    }
    return res;
}
