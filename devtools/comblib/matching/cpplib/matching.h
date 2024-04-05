#ifndef _MATCHING_H_
#define _MATCHING_H_

#include <vector>
#include <unordered_map>

typedef std::unordered_map<int, int> MATCHING_RESULT;

typedef std::pair<std::pair<int, int>, double> WEIGHTED_EDGE;

typedef std::pair<int, int> UNWEIGHTED_EDGE;

MATCHING_RESULT cpp_greedy_matching(std::vector<WEIGHTED_EDGE> edges, bool is_max);

MATCHING_RESULT cpp_hungary_matching(std::vector<UNWEIGHTED_EDGE> edges);

MATCHING_RESULT cpp_kuhn_munkres_matching(std::vector<WEIGHTED_EDGE> edges, bool is_max);


#endif