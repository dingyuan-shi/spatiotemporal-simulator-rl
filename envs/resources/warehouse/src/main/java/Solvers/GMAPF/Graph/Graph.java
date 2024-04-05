package Solvers.GMAPF.Graph;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import SimulatorCore.Path;

public class Graph {
    private  Node[] nodes;
    private ArrayList<Edge>[] adj;
    private int[][] locationToNode;

    public Node[] getNodes() {
        return this.nodes;
    }

    public ArrayList<Edge>[] getAdj() {
        return this.adj;
    }

    public Graph(String fileName) throws FileNotFoundException {
        Scanner sc = new Scanner(new FileReader(fileName));
        int n_col = 0;
        int n_row = 0;
        String line;
        while (sc.hasNextLine()) {
            ++n_row;
            line = sc.nextLine();
            n_col = line.length();
        }
        sc.close();
        sc = new Scanner(new FileReader(fileName));
        char[][] map = new char[n_row][n_col];
        this.locationToNode = new int[n_row][n_col];
        // read in the file
        Direction rackDirection = readMapFromFile(sc, map, n_row, n_col);
        sc.close();
        // generate nodes
        generateNodes(map, rackDirection, n_row, n_col);
        // generate edges
        generateEdges();
    }

    private void generateNodes(char[][] map, Direction rackDirection, int n_row, int n_col) {
        ArrayList<Node> tmpNodes = new ArrayList<>();
        Direction aisleDirection = Direction.invertDirection(rackDirection);
        ArrayList<Integer> failedRowOrCols = new ArrayList<>();
        int outterLoop = aisleDirection == Direction.ROW?n_row:n_col;
        int innerLoop = outterLoop == n_row?n_col:n_row;
        for (int i = 0; i < outterLoop; ++i) {
            int j = 0;
            for (; j < innerLoop; ++j) {
                char curLoc = aisleDirection == Direction.ROW?map[i][j]:map[j][i];
                if (curLoc == 'R') {
                    failedRowOrCols.add(i);
                    break;
                }
            }
            if (j != innerLoop) continue;
            // this row/col is an ailse
            if (aisleDirection == Direction.ROW) {
                for (int k = 0; k < innerLoop; ++k) {
                    map[i][k] = 'D';
                    locationToNode[i][k] = tmpNodes.size();
                }
                tmpNodes.add(new Node(NodeType.AISLE, i, 0, i, n_col - 1));
            } else {
                for (int k = 0; k < innerLoop; ++k) {
                    map[k][i] = 'D';
                    locationToNode[k][i] = tmpNodes.size();
                }
                tmpNodes.add(new Node(NodeType.AISLE, 0, i, n_row - 1, i));
            }
        }
        for (Integer i: failedRowOrCols) {
            for (int j = 0; j < innerLoop; ++j) {
                char type = aisleDirection == Direction.ROW?map[i][j]:map[j][i];
                if (type == 'D') continue;
                NodeType nodeType = type == 'R'?NodeType.RACK: NodeType.AISLE;
                int k = i;
                if (aisleDirection == Direction.ROW) {
                    while (k < n_row && map[k][j] == type) {
                        map[k][j] = 'D';
                        locationToNode[k][j] = tmpNodes.size();
                        ++k;
                    }
                    tmpNodes.add(new Node(nodeType, i, j, k - 1, j));
                } else {
                    while (k < n_col && map[j][k] == type) {
                        map[j][k] = 'D';
                        locationToNode[j][k] = tmpNodes.size();
                        ++k;
                    }
                    tmpNodes.add(new Node(nodeType, i, j, k - 1, j));
                }
            }
        }
        this.nodes = tmpNodes.toArray(new Node[tmpNodes.size()]);
    }

    private Direction readMapFromFile(Scanner sc, char[][] map, int n_row, int n_col) throws FileNotFoundException {
        Location firstRackLoc = null;
        for (int i = 0; i < n_row; ++i) {
            String line = sc.nextLine();
            if (line.length() != n_col) {
                System.out.println("Error, irregular layout file!!");
            }
            for (int j = 0; j < n_col; ++j) {
                map[i][j] = line.charAt(j);
                if (firstRackLoc == null && map[i][j] == 'R') {
                    firstRackLoc = new Location(i, j);
                }
            }
        }
        // judge the block direction
        int rackRow = firstRackLoc.getRow();
        int rackCol = firstRackLoc.getCol();
        Direction rackDirection = null;
        if (map[rackRow + 1][rackCol] == 'R' && map[rackRow + 2][rackCol] != 'R') {
            rackDirection = Direction.ROW;
        } else {
            rackDirection = Direction.COL;
        }
        return rackDirection;
    }

    @SuppressWarnings("unchecked")
    private void generateEdges() {
        int n_node = nodes.length;
        this.adj = new ArrayList[n_node];
        for (int i = 0; i < n_node; ++i) this.adj[i] = new ArrayList<Edge>();
        for (int i = 0; i < n_node; ++i) {
            Node n1 = nodes[i];
            for (int j = i + 1; j < n_node; ++j) {
                Node n2 = nodes[j];
                if (n1.getNodeType() == NodeType.RACK && n2.getNodeType() == NodeType.RACK) continue;
                // build edges;
                Edge n1ToN2 = n1.buildEdge(n2);
                Edge n2ToN1 = n2.buildEdge(n1);
                if (n1ToN2 != null) this.adj[i].add(n1ToN2);
                if (n2ToN1 != null) this.adj[j].add(n2ToN1);
            }
        }
    }


    @SuppressWarnings("unchecked")
    public Path planning(int currentTime, Location src, Location dst) {
        // System.out.print("#");
        PriorityQueue<Pair<Integer, Integer>> pq = new PriorityQueue<>(Comparator.comparing(Pair::getValue));
        int n = nodes.length;
        boolean[] vis = new boolean[n + 1];
        int[] dist = new int[n + 1];
        int[] preNodeId = new int[n + 1];
        Pair<Integer, Location>[] entryPoint = new Pair[n + 1];
        ArrayList<Pair<Integer, Location>>[] preTraj = new ArrayList[n + 1];
        Arrays.fill(vis, false);
        Arrays.fill(dist, -1);
        Arrays.fill(preNodeId, -1);
        int srcNodeId = locationToNode[src.getRow()][src.getCol()];
        int dstNodeId = locationToNode[dst.getRow()][dst.getCol()];
        dist[srcNodeId] = 0;
        entryPoint[srcNodeId] = Pair.of(0, src);
        preNodeId[srcNodeId] = -1;
        pq.add(Pair.of(srcNodeId, 0));
        while (!pq.isEmpty()) {
            Pair<Integer, Integer> cur = pq.poll();
            int curNodeId = cur.getKey();
            if (curNodeId == dstNodeId) {
                ArrayList<Pair<Integer, Location>> traj = this.nodes[curNodeId].getInnerTraj(currentTime, entryPoint[curNodeId].getKey(), entryPoint[curNodeId].getValue(), dst, this.nodes[curNodeId].getDirection(), this.nodes[curNodeId].getLoc1());
                if (traj == null) continue;
                int weight = traj.get(traj.size() - 1).getKey() - traj.get(0).getKey();
                if (dist[n] == -1 || dist[n] > dist[curNodeId] + weight) {
                    dist[n] = dist[curNodeId] + weight;
                    preNodeId[n] = curNodeId;
                    entryPoint[n] = traj.get(traj.size() - 1);
                    preTraj[n] = traj;
                }
                continue;
            }
            if (vis[curNodeId]) continue;
            vis[curNodeId] = true;
            for (Edge e: adj[curNodeId]) {
                // if (pq.size() == 4) System.out.println(e);
                int nextNodeId = e.getN2().getId() == curNodeId?e.getN1().getId():e.getN2().getId();
                ArrayList<Pair<Integer, Location>> traj = e.getTrajectory(currentTime, entryPoint[curNodeId], dst);
                if (traj == null) continue;
                int weight = traj.get(traj.size() - 1).getKey() - traj.get(0).getKey();
                if (dist[nextNodeId] == -1 || dist[nextNodeId] > dist[curNodeId] + weight) {
                    dist[nextNodeId] = dist[curNodeId] + weight;
                    preNodeId[nextNodeId] = curNodeId;
                    entryPoint[nextNodeId] = traj.get(traj.size() - 1);
                    preTraj[nextNodeId] = traj;
                    pq.add(Pair.of(nextNodeId, dist[nextNodeId]));
                }
            }
        }
        // parse to path
        // get the node in order
        ArrayList<Integer> nodeList = new ArrayList<>();
        int tmpNodeId = n;
        while (tmpNodeId != -1) {
            nodeList.add(tmpNodeId);
            tmpNodeId = preNodeId[tmpNodeId];
        }
        Collections.reverse(nodeList);
        if (nodeList.get(0) != srcNodeId) {
            System.out.println("QQQQ");
            ArrayList<Pair<Integer, SimulatorCore.Location>> traj = new ArrayList<>();
            traj.add(Pair.of(0, new SimulatorCore.Location(src.getRow(), src.getCol())));
            traj.add(Pair.of(src.manhattanDistance(dst), new SimulatorCore.Location(dst.getRow(), dst.getCol())));
            return new Path(traj, currentTime);
        }
        ArrayList<Pair<Integer, Location>> trajectory = new ArrayList<>();
        // parse trajectories
        for (int i = 1; i < nodeList.size(); ++i) {
            int endNodeId = nodeList.get(i);
            Node beginNode = this.nodes[nodeList.get(i - 1)];
            beginNode.insertSegment(currentTime, preTraj[endNodeId]);
            if (i != nodeList.size() - 1)
                this.nodes[endNodeId].insertSegment(currentTime, preTraj[endNodeId]); 
            Pair<Integer, Location> lastST = trajectory.size() > 0?trajectory.get(trajectory.size() - 1):null;
            for (int j = 0; j < preTraj[endNodeId].size(); ++j) {
                int t = preTraj[endNodeId].get(j).getKey();
                Location loc = preTraj[endNodeId].get(j).getValue();
                if (j == 0 && lastST != null) {
                    if (lastST.equals(preTraj[endNodeId].get(0))) continue;
                    beginNode.insertSegment(new Segment(lastST.getKey(), beginNode.getInnerLoc(lastST.getValue()), t, beginNode.getInnerLoc(lastST.getValue())));   
                }
                trajectory.add(Pair.of(t, new Location(loc.getRow(), loc.getCol())));
            }
        }
        return new Path(trajectory.stream().map(o -> Pair.of(o.getKey(), new SimulatorCore.Location(o.getValue().getRow(), o.getValue().getCol()))).collect(Collectors.toCollection(ArrayList::new)), currentTime);
    }

}
