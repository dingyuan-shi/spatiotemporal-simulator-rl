package Solvers.TWP;

import SimulatorCore.Location;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;

public class Memory {
    private static final int THRESH = 5;
    // private static final int THRESH = 100;
    private static final HashMap<String, ArrayList<Pair<Integer, Location>>> memo = new HashMap<>();

    private static String getNodePair(int srcR, int srcC, int dstR, int dstC) {
        return srcR + "@" + srcC + "@" + dstR +"@" + dstC;
    }

    private static void put(int srcR, int srcC, int dstR, int dstC, ArrayList<Pair<Integer, Location>> trajectory) {
        memo.put(getNodePair(srcR, srcC, dstR, dstC), trajectory);
    }

    public static void put(ArrayList<Pair<Integer, Location>> trajectory) {
        int startIndex = Math.max(0, trajectory.size() - THRESH);
        put(trajectory.get(startIndex).getValue().getR(), trajectory.get(startIndex).getValue().getC(),
                trajectory.get(trajectory.size() - 1).getValue().getR(), trajectory.get(trajectory.size() - 1).getValue().getC(),
                trajectory);
    }

    public static ArrayList<Pair<Integer, Location>> get(int srcR, int srcC, int dstR, int dstC) {
        if (!isPossible(srcR, srcC, dstR, dstC)) return null;
        return memo.get(getNodePair(srcR, srcC, dstR, dstC));
    }

    public static boolean isPossible(int srcR, int srcC, int dstR, int dstC) {
        return Math.abs(srcR - dstR) + Math.abs(srcC - dstC) <= THRESH;
    }

}
