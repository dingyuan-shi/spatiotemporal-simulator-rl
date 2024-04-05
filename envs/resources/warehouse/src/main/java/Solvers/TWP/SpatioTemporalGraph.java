package Solvers.TWP;

import SimulatorCore.Location;
import org.apache.commons.lang3.tuple.Pair;
import SimulatorCore.Path;
import SimulatorCore.RackGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;

public class SpatioTemporalGraph {
    private final HashSet<Integer>[][] dynamicG;
    private final HashSet<Integer>[][] visitG;
    private final int [][] staticG;
    private int currentTime;
    private final int height;
    private final int width;

    @SuppressWarnings("unchecked")
    public SpatioTemporalGraph(int height, int width, RackGroup rg) {
        this.currentTime = 0;
        this.height = height;
        this.width = width;
        dynamicG = new HashSet[height][width];
        visitG = new HashSet[height][width];
        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                dynamicG[i][j] = new HashSet<>();
                visitG[i][j] = new HashSet<>();
            }
        }
        staticG = new int[height][width];
        for (int r = 0; r < rg.getRackNum(); ++r) {
            Location rackLoc = rg.getRackById(r).getLocation();
            staticG[rackLoc.getR()][rackLoc.getC()] = 1;
        }
    }

    public int getSpatioTemporalPoint(int t, int r, int c) {
        if (t < 0 || r < 0 || r >= height || c < 0 || c >= width || staticG[r][c] == 1) return -1;
        if (dynamicG[r][c].contains(t) || dynamicG[r][c].contains(t - 1)) return -1;
        return 0;
    }

    public void insertSinglePath(Path path) {
        int startTime = path.getStartTime();
        for (Pair<Integer, Location> stp: path.getTrajectory()) {
            dynamicG[stp.getValue().getR()][stp.getValue().getC()].add(stp.getKey() + startTime);
        }
    }

    public void update(int t) {
        if (t - this.currentTime < 10) return;
        this.currentTime = t;
        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                dynamicG[i][j].removeIf(o -> o < t);
                visitG[i][j].clear();
            }
        }
    }

    public void setVis(int t, int r, int c) {
        visitG[r][c].add(t);
    }

    public boolean isVis(int t, int r, int c) {
        return visitG[r][c].contains(t);
    }

    public ArrayList<Pair<Integer, Location>> getTrajectoryAfterAdjust(int currentTime, List<Pair<Integer, Location>> trajectory) {
        ArrayList<Pair<Integer, Location>> newTrajectory = new ArrayList<>();
        int t = 0;
        for (Pair<Integer, Location> tLoc: trajectory) {
            int r = tLoc.getValue().getR();
            int c = tLoc.getValue().getC();
            while (dynamicG[r][c].contains(currentTime + t) || dynamicG[r][c].contains(currentTime + t - 1)) ++t;
            newTrajectory.add(Pair.of(++t, tLoc.getValue()));
        }
        return newTrajectory;
    }

    public static Path AStar(int currentTime, int[] src, int[] dst, SpatioTemporalGraph SPG) {
        int[] dr = {0, 0, 0, 1, -1};
        int[] dc = {0, 1, -1, 0, 0};
        int srcR = src[0];
        int srcC = src[1];
        int dstR = dst[0];
        int dstC = dst[1];
        Node lastNode = null;
        PriorityQueue<Node> q = new PriorityQueue<>(
                (o1, o2) -> o1.getH() - o2.getH() == 0?Math.abs(o1.getC() - dstC) + Math.abs(o1.getR() - dstR) -
                        (Math.abs(o2.getC() - dstC) + Math.abs(o2.getR() - dstR)):o1.getH() - o2.getH()
        );
        q.offer(new Node(srcR, srcC, 0, 0, 0, null));
        ArrayList<Pair<Integer, Location>> traj = null;
        while (!q.isEmpty() && lastNode == null) {
            Node current = q.poll();
            SPG.setVis(current.getT() + currentTime, current.getR(), current.getC());
            traj = Memory.get(current.getR(), current.getC(), dstR, dstC);
            if (traj != null) {
                traj = SPG.getTrajectoryAfterAdjust(current.getT() + currentTime, traj);
                lastNode = current;
                break;
            }
            for (int i = 0; i < 5; ++i) {
                int nextT = current.getT() + 1;
                int nextR = current.getR() + dr[i];
                int nextC = current.getC() + dc[i];
                if (nextR == dstR && nextC == dstC) {
                    lastNode = current;
                    break;
                }
                if ((nextR != srcR || nextC != srcC) && (SPG.getSpatioTemporalPoint(nextT + currentTime, nextR, nextC) == -1 || SPG.isVis(nextT + currentTime, nextR, nextC))) continue;
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
        if (traj != null) trajectory.addAll(traj);
        else Memory.put(trajectory);
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

