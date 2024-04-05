#include <vector>
#include <typeinfo> 
#include "matching.h"
#include "raw_pre_post_processing.h"


void _mapping_nodes_unweighted(const std::vector<UNWEIGHTED_EDGE>& edges, bool& is_switch, 
        std::unordered_map<int, int>& left_name_to_idx, std::unordered_map<int, int>& right_name_to_idx, 
        std::vector<int>& left_idx_to_name, std::vector<int>& right_idx_to_name) {
    // build mapping from user defined node name to inner indices.
    for (std::vector<UNWEIGHTED_EDGE>::const_iterator it = edges.begin(); it != edges.end(); ++it) {
        int left_node_name = it->first;
        int right_node_name = it->second;
        if (left_name_to_idx.find(left_node_name) == left_name_to_idx.end()) {
            left_name_to_idx[left_node_name] = left_idx_to_name.size();
            left_idx_to_name.push_back(left_node_name);
        }
        if(right_name_to_idx.find(right_node_name) == right_name_to_idx.end()) {
            right_name_to_idx[right_node_name] = right_idx_to_name.size();
            right_idx_to_name.push_back(right_node_name);
        }
    }
    // always keep the left side is smaller.
    is_switch = left_idx_to_name.size() > right_idx_to_name.size();
    if (is_switch) {
        std::swap(left_idx_to_name, right_idx_to_name);
        std::swap(left_name_to_idx, right_name_to_idx);
    }
    return;
} 

void _mapping_nodes_weighted(const std::vector<WEIGHTED_EDGE>& edges, bool& is_switch, const bool& is_max, 
        std::unordered_map<int, int>& left_name_to_idx, std::unordered_map<int, int>& right_name_to_idx, 
        std::vector<int>& left_idx_to_name, std::vector<int>& right_idx_to_name) {
    // build mapping from user defined node name to inner indices.
    for (std::vector<WEIGHTED_EDGE>::const_iterator it = edges.begin(); it != edges.end(); ++it) {
        int left_node_name = it->first.first;
        int right_node_name = it->first.second;
        double weight = it->second;
        if ((is_max && weight < 0) || (!is_max && weight > 0)) continue;
        weight = is_max?weight:-weight;
        if (left_name_to_idx.find(left_node_name) == left_name_to_idx.end()) {
            left_name_to_idx[left_node_name] = left_idx_to_name.size();
            left_idx_to_name.push_back(left_node_name);
        }
        if(right_name_to_idx.find(right_node_name) == right_name_to_idx.end()) {
            right_name_to_idx[right_node_name] = right_idx_to_name.size();
            right_idx_to_name.push_back(right_node_name);
        }
    }
    // always keep the left side is smaller.
    is_switch = left_idx_to_name.size() > right_idx_to_name.size();
    if (is_switch) {
        std::swap(left_idx_to_name, right_idx_to_name);
        std::swap(left_name_to_idx, right_name_to_idx);
    }
    return;
} 

void _build_graph_unweighted(const std::vector<UNWEIGHTED_EDGE>& edges, const bool& is_switch, 
    const std::unordered_map<int, int>& left_name_to_idx, const std::unordered_map<int, int>& right_name_to_idx, 
    std::vector<int>* adj) {
    
    for (std::vector<UNWEIGHTED_EDGE>::const_iterator it = edges.begin(); it != edges.end(); ++it) {
        int left_node_name = it->first;
        int right_node_name = it->second;
        if (is_switch) {
            std::swap(left_node_name, right_node_name);
        }
        int left_node_idx = left_name_to_idx.at(left_node_name);
        int right_node_idx = right_name_to_idx.at(right_node_name);
        adj[left_node_idx].push_back(right_node_idx);
    }
    return;
}

void _build_graph_weighted(const std::vector<WEIGHTED_EDGE>& edges, const bool& is_switch, 
    const std::unordered_map<int, int>& left_name_to_idx, const std::unordered_map<int, int>& right_name_to_idx, 
    std::vector<std::pair<int, double> >* adj) {
    for (std::vector<WEIGHTED_EDGE>::const_iterator it = edges.begin(); it != edges.end(); ++it) {
        int left_node_name = it->first.first;
        int right_node_name = it->first.second;
        double weight = it->second;
        if (is_switch) {
            std::swap(left_node_name, right_node_name);
        }
        int left_node_idx = left_name_to_idx.at(left_node_name);
        int right_node_idx = right_name_to_idx.at(right_node_name);
        adj[left_node_idx].push_back(std::make_pair(right_node_idx, weight));
    }
    return;
}

void _convert_back(MATCHING_RESULT& res, const int * lmatch, const bool& is_switch, const std::vector<int>& left_idx_to_name, const std::vector<int>& right_idx_to_name) {
    int n = left_idx_to_name.size();
    for (int u = 0; u < n; ++u) {
        if (lmatch[u] == -1) continue;
        int v = lmatch[u];
        int left_node_name = left_idx_to_name[u];
        int right_node_name = right_idx_to_name[v];
        if (is_switch) {
            std::swap(left_node_name, right_node_name);
        }
        res[left_node_name] = right_node_name;
    }
    return;
}