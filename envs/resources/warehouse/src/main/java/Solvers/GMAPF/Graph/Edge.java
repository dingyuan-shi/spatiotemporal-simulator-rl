package Solvers.GMAPF.Graph;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.lang3.tuple.Pair;

public class Edge {

    private ConnectType connectType;
    private Node n1;
    private Node n2;
    private Location[] connectPoint;

    public Edge(Node n1, Node n2, ConnectType connectType, Location cp1, Location cp2) {
        this.n1 = n1;
        this.n2 = n2;
        this.connectType = connectType;
        this.connectPoint = new Location[]{cp1, cp2};
    }

    @Override
    public String toString() {
        return "[" + n1 + "]->[" + n2 + "]" + connectType + "cp:" + Arrays.toString(connectPoint);
    }

    public ConnectType getConnectType() {
        return connectType;
    }

    public Node getN1() {
        return n1;
    }

    public Node getN2() {
        return n2;
    }

    public Location[] getConnectPoint() {
        return this.connectPoint;
    }

    public ArrayList<Pair<Integer, Location>> getTrajectory(int currentTime, Pair<Integer, Location> srcEntry, Location dst) {
        int timeOffset = srcEntry.getKey();
        Location src = srcEntry.getValue();
        if (this.connectType == ConnectType.VERTICAL) {
            if (n1.contains(src)) {
                ArrayList<Pair<Integer, Location>> traj = n1.getInnerTraj(currentTime, timeOffset, src, connectPoint[0], n1.getDirection(), n1.getLoc1());
                if (traj == null) return null;
                int stepInTime = n2.stepIn(currentTime + traj.get(traj.size() - 1).getKey() + 1, connectPoint[1]);
                traj.add(Pair.of(stepInTime - currentTime, connectPoint[1]));
                return traj;
            } 
            // n2 contains src
            ArrayList<Pair<Integer, Location>> traj = n2.getInnerTraj(currentTime, timeOffset, src, connectPoint[1], n2.getDirection(), n2.getLoc1());
            if (traj == null) return null;
            int stepInTime = n1.stepIn(currentTime + traj.get(traj.size() - 1).getKey() + 1, connectPoint[0]);
            traj.add(Pair.of(stepInTime - currentTime, connectPoint[0]));
            return traj;
        }
        // this.connectType == ConnectType.ON_SHOULDER
        Node nTo = n1.contains(src)?n2:n1;
        Node nFrom = n1.contains(src)?n1:n2;
        boolean isConvert = nTo.getDirection() == Direction.COL;
        ArrayList<Pair<Integer, Location>> traj = new ArrayList<>();
        if (nTo.getNodeType() == NodeType.RACK && nTo.contains(dst)) {
            traj = nFrom.getInnerTraj(currentTime, timeOffset, src, new Location(dst.getCol(isConvert), nFrom.getDirectionCoord(), !isConvert), nFrom.getDirection(), nFrom.getLoc1());
        } else {
            traj.add(Pair.of(timeOffset, src));
        }
        Location dst2 = new Location(nTo.getDirectionCoord(), src.getCol(isConvert), isConvert);
        int stepInTime = nTo.stepIn(currentTime + timeOffset + 1, dst2);
        traj.add(Pair.of(stepInTime - currentTime, dst2));
        return traj;
    }
    
}
