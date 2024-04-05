package testGAMPF;

import java.io.FileNotFoundException;
import java.util.ArrayList;

import org.junit.Test;

import Solvers.GMAPF.Graph.Edge;
import Solvers.GMAPF.Graph.Graph;
import Solvers.GMAPF.Graph.Node;

public class testGraph {
    // @Test
    // public void testGraphConstructor() throws FileNotFoundException {
    //     Graph g = new Graph("/fastMAPF/data/layout.txt");
    //     System.out.println(g.getNodes().length);
    //     ArrayList<Edge>[] adj = g.getAdj();
    //     Node[] nodes = g.getNodes();
    //     int n_edges = 0;
    //     for (int i = 0; i < adj.length; ++i) {
    //         System.out.println(i + " " + nodes[i]);
    //         ArrayList<Edge> edges = adj[i];
    //         n_edges += edges.size();
    //         for (Edge e: edges) {
    //             System.out.println(e);
    //         }
    //         System.out.println();
    //     }
    //     System.out.printf("n_nodes: %d, n_edges: %d\n", nodes.length, n_edges);
    // }
}
