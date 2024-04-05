package Solvers.SAP;

import SimulatorCore.Location;
import org.apache.commons.lang3.tuple.Pair;
import SimulatorCore.Path;
import SimulatorCore.RackGroup;

import java.util.*;

public class NaiveSpatioTemporalGraph {
    private int[][][] dynamicG;
    private int [][] staticG;
    private int currentTime;
    private int currentIndex;
    private int height;
    private int width;
    private static final int MAX_SPAN = 5000;

    public NaiveSpatioTemporalGraph(int height, int width, RackGroup rg) {
        this.currentTime = 0;
        this.currentIndex = 0;
        this.height = height;
        this.width = width;
        dynamicG = new int[MAX_SPAN][height][width];
        staticG = new int[height][width];
        for (int r = 0; r < rg.getRackNum(); ++r) {
            Location rackLoc = rg.getRackById(r).getLocation();
            staticG[rackLoc.getR()][rackLoc.getC()] = 1;
        }
    }

    public int getSpatioTemporalPoint(int t, int r, int c) {
        // illegal or conflict on racks
        if (t < 0 || r < 0 || r >= height || c < 0 || c >= width || staticG[r][c] == 1) return -1;
        // check path
        if (dynamicG[(currentIndex + t - currentTime) % MAX_SPAN][r][c] == 1) return -1;
        return 0;
    }

    public void insertPaths(Set<Path> paths) {
        for (Path path: paths) {
            insertSinglePath(path);
        }
    }

    public void insertSinglePath(Path path) {
        int startTime = path.getStartTime();
        for (Pair<Integer, Location> stp: path.getTrajectory()) {
            dynamicG[(currentIndex + stp.getKey() + startTime - this.currentTime) % MAX_SPAN][stp.getValue().getR()][stp.getValue().getC()] = 1;
        }
    }

    public void update(int t) {
        int deltaT = t - currentTime;
        currentTime = t;
        for (int i = 0; i < deltaT; i++) {
            for (int j = 0; j < dynamicG[0].length; ++j)
                Arrays.fill(dynamicG[(currentIndex + i) % MAX_SPAN][j], 0);
        }
        currentIndex = (currentIndex + deltaT) % MAX_SPAN;
    }

    public int getHeight() {
        return this.height;
    }

    public int getWidth() {
        return this.width;
    }

    public static Path AStar(int currentTime, int[] src, int[] dst, NaiveSpatioTemporalGraph SPG) {
        int width = SPG.getWidth();
        int height = SPG.getHeight();
        int[] dr = {0, 0, 0, 1, -1};
        int[] dc = {0, 1, -1, 0, 0};
        int srcR = src[0];
        int srcC = src[1];
        int dstR = dst[0];
        int dstC = dst[1];
        Node lastNode = null;
        PriorityQueue<Node> q = new PriorityQueue<>(Comparator.comparingInt(Node::getH));
        boolean[][][] vis = new boolean[5000][height][width];
        q.offer(new Node(srcR, srcC, 0, 0, 0, null));
        while (!q.isEmpty() && lastNode == null) {
            Node current = q.poll();
            vis[current.getT()][current.getR()][current.getC()] = true;
            for (int i = 0; i < 5; ++i) {
                int nextT = current.getT() + 1;
                int nextR = current.getR() + dr[i];
                int nextC = current.getC() + dc[i];
                if (nextR == dstR && nextC == dstC) {
                    lastNode = current;
                    break;
                }
                if ((nextR != srcR || nextC != srcC) && (SPG.getSpatioTemporalPoint(nextT + currentTime, nextR, nextC) == -1 || vis[nextT][nextR][nextC])) continue;
                int nextG = current.getG() + Math.abs(nextR - current.getR()) + Math.abs(nextC - current.getC());
                int nextH = Math.abs(nextR - dstR) + Math.abs(nextC - dstC) + nextT;
                Node recN = null;
                for (Node n: q) {
                    if (n.getR() == nextR && n.getC() == nextC && n.getT() == nextT) {
                        recN = n;
                        break;
                    }
                }
                if (recN != null) {
                    if (recN.getG() > nextG) {
                        recN.setG(nextG);
                        recN.setPre(current);
                    }
                } else {
                    q.offer(new Node(nextR, nextC, nextT, nextG, nextH, current));
                }
            }
        }
        ArrayList<Pair<Integer, Location>> trajectory = new ArrayList<>();
        trajectory.add(Pair.of(lastNode.getT() + 1, new Location(dstR, dstC)));
        while (lastNode != null) {
            trajectory.add(Pair.of(lastNode.getT(), new Location(lastNode.getR(), lastNode.getC())));
            lastNode = lastNode.getPre();
        }
        Collections.reverse(trajectory);
        return new Path(trajectory, currentTime);
    }
}


class Node {
    private int r;
    private int c;
    private int t;
    private int g;
    private int h;
    private Node pre;

    public Node(int r, int c, int t, int g, int h, Node pre) {
        this.r = r;
        this.c = c;
        this.t = t;
        this.g = g;
        this.h = h;
        this.pre = pre;
    }

    public int getR() {
        return r;
    }

    public int getC() {
        return c;
    }

    public int getT() {
        return t;
    }

    public int getG() {
        return g;
    }

    public int getH() {
        return h;
    }

    public Node getPre() {
        return pre;
    }

    public void setR(int r) {
        this.r = r;
    }

    public void setC(int c) {
        this.c = c;
    }

    public void setT(int t) {
        this.t = t;
    }

    public void setG(int g) {
        this.g = g;
    }

    public void setH(int h) {
        this.h = h;
    }

    public void setPre(Node pre) {
        this.pre = pre;
    }
}
