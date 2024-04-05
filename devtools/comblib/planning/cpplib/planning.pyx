from libcpp cimport int
from libcpp.vector cimport vector
from libcpp.pair cimport pair
import typing


cdef extern from "planning.h":
    ctypedef vector[int] PLANNING_RESULT
    ctypedef vector[vector[pair[int, double]]] WEIGHTED_GRAPH;
    PLANNING_RESULT cpp_bfs_planning(WEIGHTED_GRAPH graph, int src, int dst);
    PLANNING_RESULT cpp_dfs_planning(WEIGHTED_GRAPH graph, int src, int dst);
    PLANNING_RESULT cpp_dijkstra_planning(WEIGHTED_GRAPH graph, int src, int dst);

def bfs(graph: list[list[tuple[typing.int, float]]], src: typing.int, dst: typing.int) -> list[typing.int]:
    return cpp_bfs_planning(graph, src, dst);

def dfs(graph: list[list[tuple[typing.int, float]]], src: typing.int, dst: typing.int) -> list[typing.int]:
    return cpp_dfs_planning(graph, src, dst);

def dijkstra(graph: list[list[tuple[typing.int, float]]], src: typing.int, dst: typing.int) -> list[typing.int]:
    return cpp_dijkstra_planning(graph, src, dst);
