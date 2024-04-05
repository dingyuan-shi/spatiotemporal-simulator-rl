package Solvers.ACP;

import SimulatorCore.Location;
import org.apache.commons.lang3.tuple.Pair;
import SimulatorCore.Path;
import SimulatorCore.RackGroup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class SpatioTemporalGraph {
    private final HashSet<Integer>[][] dynamicG;
    private final HashSet<Integer>[][] visitG;
    private final int [][] staticG;
    private int currentTime;
    private final int height;
    private final int width;

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
}
