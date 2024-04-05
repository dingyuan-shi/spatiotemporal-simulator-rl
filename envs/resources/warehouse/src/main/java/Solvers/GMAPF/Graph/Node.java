package Solvers.GMAPF.Graph;

import java.util.ArrayList;

import org.apache.commons.lang3.tuple.Pair;

public class Node {

    private static int cnt = 0;
    private NodeType nodeType;
    private Direction direction;
    private int length;
    private Location loc1;
    private Location loc2;
    private int directionCoord;
    private int id;
    private SegmentIndex sgi;

    public int getDirectionCoord() {
        return this.directionCoord;
    }

    public Location getLoc1() {
        return this.loc1;
    }

    public Location getLoc2() {
        return this.loc2;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public Direction getDirection() {
        return direction;
    }

    public int getLength() {
        return length;
    }

    public void insertSegment(Segment s) {
        this.sgi.insertSegment(s);
    }

    public void insertSegment(int currentTime, ArrayList<Pair<Integer, Location>> traj) {
        for (int i = 1; i < traj.size(); ++i) {
            Location src = traj.get(i - 1).getValue();
            int tsrc = currentTime + traj.get(i - 1).getKey();
            Location dst = traj.get(i).getValue();
            int tdst = currentTime + traj.get(i).getKey();
            if (this.contains(src) && this.contains(dst)) {
                this.sgi.insertSegment(new Segment(tsrc, this.getInnerLoc(src), tdst, this.getInnerLoc(dst)));
            } else {
                if (this.contains(src)) {
                    this.sgi.insertSegment(new Segment(tsrc, this.getInnerLoc(src), tsrc, this.getInnerLoc(src)));
                }
                if (this.contains(dst)) {
                    this.sgi.insertSegment(new Segment(tdst, this.getInnerLoc(dst), tdst, this.getInnerLoc(dst)));
                }
            }
        }
    }

    public int getInnerLoc(Location a) {
        if (this.direction == Direction.COL) {
            return a.getRow() - this.loc1.getRow();
        }
        return a.getCol() - this.loc1.getCol();
    }

    public Node(NodeType nodeType, Location startLoc, Location endLoc) {
        int startRow = startLoc.getRow();
        int startCol = startLoc.getCol();
        int endRow = endLoc.getRow();
        int endCol = endLoc.getCol(); 
        new Node(nodeType, startRow, startCol, endRow, endCol);
    }

    public boolean contains(Location loc) {
        return loc.getRow() >= loc1.getRow() && loc.getRow() <= loc2.getRow() && loc.getCol() >= loc1.getCol() && loc.getCol() <= loc2.getCol();
    }

    public int getId() {
        return this.id;
    }

    public Node(NodeType nodeType, int startRow, int startCol, int endRow, int endCol) {
        this.id = cnt++;
        this.nodeType = nodeType;
        // check if not parallel or vertical
        assert (startRow == endRow || startCol == endCol);
        if (startRow == endRow) {
            directionCoord = startRow;
            this.direction = Direction.ROW;
            this.length = Math.abs(startCol - endCol) + 1;
        } else {
            // startCol == endCol
            directionCoord = startCol;
            this.direction = Direction.COL;
            this.length = Math.abs(startRow - endRow) + 1;
        }
        this.loc1 = new Location(Math.min(startRow, endRow), Math.min(startCol, endCol));
        this.loc2 = new Location(Math.max(startRow, endRow), Math.max(startCol, endCol));
        this.sgi = new SegmentIndex(this.length);
    }

    @Override
    public String toString() {
        return nodeType + ", " + direction + ", " + length + loc1 + "->" + loc2;
    }

    public Edge buildEdge(Node n2) {
        if (this.direction == n2.getDirection()) {
            boolean isConvert = this.direction == Direction.COL;
            // check row
            int gap = this.getDirectionCoord() - n2.getDirectionCoord();
            switch(gap) {
                case 1:
                    // shoulder on shoulder n2 on top
                    if (this.loc1.getCol(isConvert) == n2.getLoc1().getCol(isConvert) && this.loc2.getCol(isConvert) == n2.getLoc2().getCol(isConvert)) {
                        return new Edge(n2, this, ConnectType.ON_SHOULDER, n2.getLoc1(), this.loc1);
                    }
                break;
                case -1:
                    // shoulder on shoulder this on top
                    if (this.loc1.getCol(isConvert) == n2.getLoc1().getCol(isConvert) && this.loc2.getCol(isConvert) == n2.getLoc2().getCol(isConvert)) {
                        return new Edge(this, n2, ConnectType.ON_SHOULDER, this.loc1, n2.getLoc1());
                    }
                default:
                    return null;
            }
        } else {
            boolean isConvert = this.direction == Direction.COL;
            int gap = n2.getDirectionCoord() - this.loc1.getCol(isConvert);
            if (gap < -1 || gap > this.getLength()) return null;
            Location interSection = new Location(this.getDirectionCoord(), n2.getDirectionCoord(), isConvert);
            if (gap == -1 || gap == this.getLength()) {
                // |= or =|
                if (this.getDirectionCoord() >= n2.getLoc1().getRow(isConvert) && this.getDirectionCoord() <= n2.getLoc2().getRow(isConvert)) {
                    return new Edge(this, n2, ConnectType.VERTICAL, gap==-1?this.loc1:this.loc2, interSection);
                }
            } else if (gap >= 0 && gap < this.getLength()) {
                // =|=  this on top  T
                if (n2.getLoc1().getRow() == this.getDirectionCoord() + 1)
                    return new Edge(n2, this, ConnectType.VERTICAL, n2.getLoc1(), interSection);
                else if (n2.getLoc2().getRow() == this.getDirectionCoord() - 1)
                    return new Edge(n2, this, ConnectType.VERTICAL, n2.getLoc2(), interSection);
            }
        }
        return null;
    }

    public ArrayList<Pair<Integer, Location>> getInnerTraj(int currentTime, int timeOffset, Location src, Location dst, Direction dir, Location n1)  {
        int srcOneDimLoc = src.getRow(this.direction == Direction.ROW) - this.loc1.getRow(this.direction == Direction.ROW);
        int dstOneDimLoc = dst.getRow(this.direction == Direction.ROW) - this.loc1.getRow(this.direction == Direction.ROW);
        if (srcOneDimLoc == dstOneDimLoc) {
            ArrayList<Pair<Integer, Location>> traj = new ArrayList<>();
            traj.add(Pair.of(timeOffset, dst));
            return traj;
        }
        return this.sgi.query(currentTime, timeOffset, srcOneDimLoc, dstOneDimLoc, dir, n1);        
    }

    public int stepIn(int showUpTime, Location dst) {
        int dstOneDimLoc = dst.getRow(this.direction == Direction.ROW) - this.loc1.getRow(this.direction == Direction.ROW);
        return this.sgi.query(showUpTime, dstOneDimLoc);
    }
}
