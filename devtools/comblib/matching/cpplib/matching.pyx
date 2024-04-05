from libcpp cimport bool, int
from libcpp.vector cimport vector
from libcpp.unordered_map cimport unordered_map
from libcpp.pair cimport pair
import typing


cdef extern from "matching.h":
    ctypedef unordered_map[int, int] MATCHING_RESULT
    ctypedef pair[pair[int, int], double] WEIGHTED_EDGE
    ctypedef pair[int, int] UNWEIGHTED_EDGE
    MATCHING_RESULT cpp_greedy_matching(vector[WEIGHTED_EDGE] edges, bool is_max)
    MATCHING_RESULT cpp_hungary_matching(vector[UNWEIGHTED_EDGE] edges)
    MATCHING_RESULT cpp_kuhn_munkres_matching(vector[WEIGHTED_EDGE] edges, bool is_max) 


def greedy(edges: tuple[tuple[typing.int, typing.int], float], is_max: typing.bool=True) -> dict[typing.int, typing.int]:
    return cpp_greedy_matching(edges, is_max)

def hungary(edges: tuple[typing.int, typing.int]) -> dict[typing.int, typing.int]:
    return cpp_hungary_matching(edges)

def kuhn_munkres(edges: tuple[tuple[typing.int, typing.int], float], is_max: typing.bool=True) -> dict[typing.int, typing.int]:
    return cpp_kuhn_munkres_matching(edges, is_max);