#include "raw_pre_post_processing.h"
#include "planning.h"
#include <algorithm>


PLANNING_RESULT get_res_from_pre(const int * pre, int dst) {
    PLANNING_RESULT res;
    if (pre[dst] == -1) return res;
    int tmp = dst;
    while(tmp != -1) {
        res.push_back(tmp);
        tmp = pre[tmp];
    }
    std::reverse(res.begin(), res.end());
    return res;
}