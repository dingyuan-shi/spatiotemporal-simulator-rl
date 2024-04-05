#ifndef _RAW_PRE_POST_PROCESSING_H_
#define _RAW_PRE_POST_PROCESSING_H_

#include <vector>
#include "matching.h"

void _mapping_nodes_unweighted(const std::vector<UNWEIGHTED_EDGE>& , bool& , std::unordered_map<int, int>& , std::unordered_map<int, int>& , 
        std::vector<int>& , std::vector<int>& );

void _mapping_nodes_weighted(const std::vector<WEIGHTED_EDGE>& , bool& , const bool&, std::unordered_map<int, int>& , std::unordered_map<int, int>& , 
        std::vector<int>& , std::vector<int>& );

void _build_graph_unweighted(const std::vector<UNWEIGHTED_EDGE>& , const bool& , const std::unordered_map<int, int>& , const std::unordered_map<int, int>& , 
    std::vector<int>* );

void _build_graph_weighted(const std::vector<WEIGHTED_EDGE>& , const bool& , const std::unordered_map<int, int>& , const std::unordered_map<int, int>& , 
    std::vector<std::pair<int, double> >* );

void _convert_back(MATCHING_RESULT& res, const int * lmatch, const bool& is_switch, const std::vector<int>& left_idx_to_name, const std::vector<int>& right_idx_to_name);

#endif