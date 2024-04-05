package Solvers.GMAPF.Graph;
import static Solvers.GMAPF.Graph.Location.crossMultiplication;
import static Solvers.GMAPF.Graph.Location.sub;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class SegmentIndex {
    // private ArrayList<Segment> segments;
    private ArrayList<Segment> horizenSegments;
    private ArrayList<Segment> upperSegments;
    private ArrayList<Segment> orderedUpperSegments;
    private ArrayList<Segment> downSegments;
    private ArrayList<Segment> orderedDownSegments;
    private HashMap<Integer, HashSet<Integer>> upperLocToIndex;
    private HashMap<Integer, HashSet<Integer>> downLocToIndex;
    private int lastClear;

    public SegmentIndex(int len) {
        // this.segments = new ArrayList<>();
        this.horizenSegments = new ArrayList<>();
        this.upperSegments = new ArrayList<>();
        this.downSegments = new ArrayList<>();
        this.orderedDownSegments = new ArrayList<>();
        this.orderedUpperSegments = new ArrayList<>();
        this.upperLocToIndex = new HashMap<>();
        this.downLocToIndex = new HashMap<>();
        this.lastClear = -1;
    }

    // public void insertSegment(Segment s) {
    //     for (int i = 0; i < horizenSegments.size(); ++i) {
    //         if (horizenSegments.get(i).contains(s)) {
    //             return;
    //         }
    //     }
    //     horizenSegments.add(s);
    //     horizenSegments.sort((Segment a, Segment b)->{return a.getStartTimeSpatio().getRow() - b.getStartTimeSpatio().getRow();});
    // }

    // public void clear(int currentTime) {
    //     if (currentTime - lastClear < 50) return;
    //     horizenSegments = clearSegments(currentTime, horizenSegments);
    //     lastClear = currentTime;
    // }

    // private int findCollision(int tsrc, int src, int tdst, int dst) {
    //     return findCollisionSingle(tsrc, src, tdst, dst, horizenSegments);
    // }

    public void insertSegment(Segment s) {
        int dir = s.getDirection();
        ArrayList<Segment> segments = dir == 0?horizenSegments:(dir > 0?upperSegments:downSegments);
        for (int i = 0; i < segments.size(); ++i) {
            if (segments.get(i).contains(s)) {
                return;
            }
        }
        
        if (dir == 0) {
            // int low = lowerBound(horizenSegments, s.getStartTimeSpatio().getRow());
            // horizenSegments.add(low, s);
            horizenSegments.add(s);
            horizenSegments.sort((Segment a, Segment b)->{return a.getStartTimeSpatio().getRow() - b.getStartTimeSpatio().getRow();});
        } else if (dir > 0) {
            // int low = lowerBound(orderedUpperSegments, s.getStartTimeSpatio().getRow());
            // orderedUpperSegments.add(low, s);
            orderedUpperSegments.add(s);
            orderedUpperSegments.sort((Segment a, Segment b)->{return a.getStartTimeSpatio().getRow() - b.getStartTimeSpatio().getRow();});

            upperSegments.add(s);
            int coo = s.getStartTimeSpatio().getRow() - s.getStartTimeSpatio().getCol();
            if (!upperLocToIndex.containsKey(coo)) {
                upperLocToIndex.put(coo, new HashSet<>());
            }
            upperLocToIndex.get(coo).add(upperSegments.size() - 1);
        } else {
            // int low = lowerBound(orderedDownSegments, s.getStartTimeSpatio().getRow());
            // orderedDownSegments.add(low, s);
            
            orderedDownSegments.add(s);
            orderedDownSegments.sort((Segment a, Segment b)->{return a.getStartTimeSpatio().getRow() - b.getStartTimeSpatio().getRow();});

            downSegments.add(s);
            int coo = s.getStartTimeSpatio().getRow() + s.getStartTimeSpatio().getCol();
            if (!downLocToIndex.containsKey(coo)) {
                downLocToIndex.put(coo, new HashSet<>());
            }
            downLocToIndex.get(coo).add(downLocToIndex.size() - 1);
        }
    }

    public void clear(int currentTime) {
        if (currentTime - lastClear < 50) return;
        horizenSegments = clearSegments(currentTime, horizenSegments);
        upperSegments = clearSegments(currentTime, upperSegments);
        orderedUpperSegments = clearSegments(currentTime, orderedUpperSegments);
        downSegments = clearSegments(currentTime, downSegments);
        orderedDownSegments = clearSegments(currentTime, orderedDownSegments);

        upperLocToIndex.clear();
        for (int i = 0; i < upperSegments.size(); ++i) {
            Segment s = upperSegments.get(i);
            int coo = s.getStartTimeSpatio().getRow() - s.getStartTimeSpatio().getCol();
            if (!upperLocToIndex.containsKey(coo)) upperLocToIndex.put(coo, new HashSet<>());
            upperLocToIndex.get(coo).add(i);
        }

        downLocToIndex.clear();
        for (int i = 0; i < downSegments.size(); ++i) {
            Segment s = downSegments.get(i);
            int coo = s.getStartTimeSpatio().getRow() + s.getStartTimeSpatio().getCol();
            if (!downLocToIndex.containsKey(coo)) downLocToIndex.put(coo, new HashSet<>());
            downLocToIndex.get(coo).add(i);
        }
        
        lastClear = currentTime;
    }

    private int findCollision(int tsrc, int src, int tdst, int dst) {
        Segment s = new Segment(tsrc, src, tdst, dst);
        int dir = s.getDirection();
        int collision = Integer.MAX_VALUE;
        if (dir == 0) {
            collision = Math.min(collision, findCollisionSingle(tsrc, src, tdst, dst, horizenSegments));
            collision = Math.min(collision, findCollisionSingle(tsrc, src, tdst, dst, orderedUpperSegments));
            collision = Math.min(collision, findCollisionSingle(tsrc, src, tdst, dst, orderedDownSegments));
        } else if (dir > 0) {
            int coo = s.getStartTimeSpatio().getRow() - s.getStartTimeSpatio().getCol();
            if (upperLocToIndex.containsKey(coo)) {
                for (Integer i: upperLocToIndex.get(coo)) {
                    collision = Math.min(collision, s.intersectionTime(upperSegments.get(i)));
                }
            }
            collision = Math.min(collision, findCollisionSingle(tsrc, src, tdst, dst, horizenSegments));
            collision = Math.min(collision, findCollisionSingle(tsrc, src, tdst, dst, orderedDownSegments));
        } else {
            int coo = s.getStartTimeSpatio().getRow() + s.getStartTimeSpatio().getCol();
            if (downLocToIndex.containsKey(coo)) {
                for (Integer i: downLocToIndex.get(coo)) {
                    collision = Math.min(collision, s.intersectionTime(downSegments.get(i)));
                }
            }
            collision = Math.min(collision, findCollisionSingle(tsrc, src, tdst, dst, horizenSegments));
            collision = Math.min(collision, findCollisionSingle(tsrc, src, tdst, dst, orderedUpperSegments));
        }
        return collision == Integer.MAX_VALUE?-1:collision;
    }


    private ArrayList<Segment> clearSegments(int currentTime, ArrayList<Segment> segments) {
        ArrayList<Segment> newSegments = new ArrayList<>();
        for (int i = 0; i < segments.size(); ++i) {
            if (!(segments.get(i).getFinishTimeSpatio().getRow() < currentTime)) {
                newSegments.add(segments.get(i));
            }
        }
        return newSegments;
    }

    public ArrayList<Pair<Integer, Location>> query(int currentTime, int timeOffset, int src, int dst, Direction dir, Location n1) {
        this.clear(currentTime);
        ArrayList<Segment> segs = new ArrayList<>();
        int showUpTime = currentTime + timeOffset;
        // the most compilicate
        ArrayList<Segment> tmp = new ArrayList<>();
        boolean hasPath = search(showUpTime, src, dst, tmp);
        if (!hasPath) return null;
        segs.add(tmp.get(0));
        for (int i = 1; i < tmp.size(); ++i) {
            segs.add(new Segment(tmp.get(i - 1).getFinishTimeSpatio(), tmp.get(i).getStartTimeSpatio()));
            segs.add(tmp.get(i));
        }
        return parseSegmentToTrajectories(currentTime, segs, dir, n1);
    }

    private boolean search(int rowStart, int src, int dst, ArrayList<Segment> tmp) {
        int delta = dst - src > 0?1:-1;
        int upper = findCollision(rowStart, src, Integer.MAX_VALUE, src);
        upper = upper == -1?rowStart + 100:upper;
        for (int t = rowStart; t < upper; ++t) {
            int collision = findCollision(t, src, t + delta * (dst - src), dst);
            if (collision == -1) {
                tmp.add(new Segment(t, src, t + delta * (dst - src), dst));
                return true;
            } else {
                for (int j = collision - 1; j > t; --j) {
                    tmp.add(new Segment(t, src, j, src + delta * (j - t)));
                    if(search(j, src + delta * (j - t), dst, tmp)) {
                        return true;
                    }
                    tmp.remove(tmp.size() - 1);
                }
            }
        }
        return false;
    }

    private static int lowerBound(ArrayList<Segment> segments, int target){
        int l = 0, r = segments.size();
        while(l < r) {
            int m = (l + r) >> 1;
            if(segments.get(m).getStartTimeSpatio().getRow() >= target) r = m;
            else l = m +1;
        }
        return l;
    }

    private static int upperBound(ArrayList<Segment> segments, int target){
        int l = 0, r = segments.size();
        while(l < r){
            int m = (l + r) >> 1;
            if(segments.get(m).getStartTimeSpatio().getRow() <= target) l = m + 1;
            else r = m;
        }
        return l;
    }

    private int findCollisionSingle(int tsrc, int src, int tdst, int dst, ArrayList<Segment> segments) {
        // to find the given segment earliest collision time
        int collision = -1;
        Segment curSeg = new Segment(tsrc, src, tdst, dst);
        int start = lowerBound(segments, tsrc);
        int finish = upperBound(segments, tdst);
        for (int i = start; i < finish; ++i) {
            if (collision == -1 || collision > curSeg.intersectionTime(segments.get(i))) {
                collision = curSeg.intersectionTime(segments.get(i));
            }
        }
        return collision;
    }

    private ArrayList<Pair<Integer, Location>> parseSegmentToTrajectories(int currentTime, ArrayList<Segment> segs, Direction dir, Location n1) {
        ArrayList<Pair<Integer, Location>> traj = new ArrayList<>();
        for (int i = 0; i < segs.size(); ++i) {
            int t = segs.get(i).getStartTimeSpatio().getRow() - currentTime;
            Location loc = new Location(n1.getRow(dir == Direction.COL), n1.getCol(dir == Direction.COL) + segs.get(i).getStartTimeSpatio().getCol(), dir == Direction.COL);
            traj.add(Pair.of(t, loc));
        }
        int t = segs.get(segs.size() - 1).getFinishTimeSpatio().getRow() - currentTime;
        Location loc = new Location(n1.getRow(dir == Direction.COL), n1.getCol(dir == Direction.COL) + segs.get(segs.size() - 1).getFinishTimeSpatio().getCol(), dir == Direction.COL);
        traj.add(Pair.of(t, loc));
        return traj;
    }

    public int query(int showUpTime, int dst) {
        // find the latest the show up available
        // should promise that it can wait any longer
        // a line find the minimum intersection
        // 快速找到从(showUptime, dst) 为端点的射线与现有线段的最大交点，然后找到+1的位置
        // return Math.max(showUpTime, this.latest[dst] + 1);
        int showTmp = showUpTime;
        while (findCollision(showTmp, dst, showTmp, dst) != -1) {
            ++showTmp;
        }
        return showTmp;
    }
}

class Segment {
    // row is time, col is space
    private Location startTimeSpatio;
    private Location finishTimeSpatio;
    

    public Segment(Location startTimeSpatio, Location finishTimeSpatio) {
        this.startTimeSpatio = startTimeSpatio;
        this.finishTimeSpatio = finishTimeSpatio;
    }

    public Segment(int a, int b, int c, int d) {
        this.startTimeSpatio = new Location(a, b);
        this.finishTimeSpatio = new Location(c, d);
    }

    public int getDirection() {
        return this.finishTimeSpatio.getCol() - this.startTimeSpatio.getCol();
    }

    public Location getStartTimeSpatio() {
        return startTimeSpatio;
    }

    public Location getFinishTimeSpatio() {
        return finishTimeSpatio;
    }

    public boolean intersection(Segment other) {
        Location a = this.startTimeSpatio;
        Location b = this.finishTimeSpatio;
        Location c = other.getStartTimeSpatio();
        Location d = other.getFinishTimeSpatio();
        
        int a_x = a.getRow();
        int a_y = a.getCol();
        int b_x = b.getRow();
        int b_y = b.getCol();
        int c_x = c.getRow();
        int c_y = c.getCol();
        int d_x = d.getRow();
        int d_y = d.getCol();

        if (Math.max(c_x, d_x) < Math.min(a_x, b_x) || Math.max(a_x, b_x) < Math.min(c_x, d_x) || Math.max(c_y, d_y) < Math.min(a_y, b_y) || Math.max(a_y, b_y) < Math.min(c_y, d_y)) return false;
        if(crossMultiplication(sub(a, d), sub(c, d)) * crossMultiplication(sub(b, d), sub(c, d)) > 0 || crossMultiplication(sub(d, b), sub(a, b)) * crossMultiplication(sub(c, b), sub(a, b)) > 0)
            return  false;
        return true;
    }

    public boolean contains(Segment other) {
        return this.startTimeSpatio.getRow() <= other.startTimeSpatio.getRow() &&   
               this.finishTimeSpatio.getRow() >= other.finishTimeSpatio.getRow() &&  // 时间上包含
               (this.startTimeSpatio.getCol() <= this.finishTimeSpatio.getCol() && 
               other.startTimeSpatio.getCol() <= other.finishTimeSpatio.getCol() &&  // 方向一致为正
               this.startTimeSpatio.getCol() <= other.startTimeSpatio.getCol() && 
               this.finishTimeSpatio.getCol() >= other.finishTimeSpatio.getCol()) ||
               (this.startTimeSpatio.getCol() >= this.finishTimeSpatio.getCol() && 
               other.startTimeSpatio.getCol() >= other.finishTimeSpatio.getCol() &&   // 方向一致为负
               this.startTimeSpatio.getCol() >= other.finishTimeSpatio.getCol() &&
               this.finishTimeSpatio.getCol() <= other.finishTimeSpatio.getCol());
    }

    private boolean isPoint() {
        return this.startTimeSpatio.getRow() == this.finishTimeSpatio.getRow();
    }

    private boolean isHorizon() {
        return !isPoint() && this.startTimeSpatio.getCol() == this.finishTimeSpatio.getCol();
    }

    public int intersectionTime(Segment other) {
        if (!intersection(other)) return -1;
        if (isPoint() || other.isPoint()) return isPoint()?this.startTimeSpatio.getRow():other.getStartTimeSpatio().getRow();
        if (isHorizon() && other.isHorizon()) return other.getStartTimeSpatio().getRow();
        if (isHorizon()) return other.getStartTimeSpatio().getRow() + Math.abs(other.getStartTimeSpatio().getCol() - this.getStartTimeSpatio().getCol());
        if (other.isHorizon()) return this.getStartTimeSpatio().getRow() + Math.abs(this.getStartTimeSpatio().getCol() - other.getStartTimeSpatio().getCol());
        return (this.startTimeSpatio.getRow() + other.getStartTimeSpatio().getRow() + Math.abs(this.startTimeSpatio.getCol() - other.getStartTimeSpatio().getCol())) / 2;
    }    
    
    @Override
    public String toString() {
        return startTimeSpatio.toString() + "->" + finishTimeSpatio.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Segment) {
            Segment tmp = (Segment) o;
            if (tmp.getStartTimeSpatio().equals(startTimeSpatio) && tmp.getFinishTimeSpatio().equals(finishTimeSpatio))
                return true;
        }
        return false;
    }
}
