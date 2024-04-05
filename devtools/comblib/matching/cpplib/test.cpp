#include "matching.h"
#include <iostream>


int main() {
    double matrix[5][8] = {{9.44421852, 8.57954403, 5.20571581, 3.5891675,  6.11274721, 5.04934137, 8.83798589, 4.03312726}, 
    {5.76596954, 6.83382039, 10.08112885, 6.04686856, 3.81837844,  8.55804204, 7.18368997, 3.50506341},
 {10.09746256, 10.82785476, 9.10217236, 10.0216595,  4.10147569,  8.29831748, 9.98838288, 7.83983932},
 {5.72142715, 2.00701208, 5.34171835, 7.10886973, 10.13011053, 10.66606368, 5.77009777, 9.65309928}, 
 {3.6049231,  9.05027827, 6.48699304, 1.140417,   8.19704686,  4.98823542, 9.24844977, 7.68153201}};
    std::vector<WEIGHTED_EDGE> edges;
    for (int i = 0; i < 5; ++i) {
        for (int j = 0; j < 8; ++j) {
            edges.push_back(std::make_pair(std::make_pair(i, j), matrix[i][j]));
        }
    }
    MATCHING_RESULT res = cpp_kuhn_munkres_matching(edges, true);
    for (MATCHING_RESULT::iterator it = res.begin(); it != res.end(); ++it) {
        std::cout << it->first << ":" << it->second << std::endl;
    }

    // UNWEIGHTED_EDGE edge1 = {0, 1};
    // UNWEIGHTED_EDGE edge2 = {1, 2};
    // UNWEIGHTED_EDGE edge3 = {2, 1};
    // UNWEIGHTED_EDGE edge4 = {3, 0};
    // std::vector<UNWEIGHTED_EDGE> edges = {edge1, edge2, edge3, edge4};
    // MATCHING_RESULT res = cpp_hungary_matching(edges);
    // for (MATCHING_RESULT::iterator it = res.begin(); it != res.end(); ++it) {
    //     std::cout << it->first << ":" << it->second << std::endl;
    // }
    return 0;
}