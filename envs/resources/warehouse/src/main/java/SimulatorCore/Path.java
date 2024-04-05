package SimulatorCore;
import org.apache.commons.lang3.tuple.Pair;
import java.util.ArrayList;
import com.alibaba.fastjson.annotation.JSONField;



public class Path {
    private ArrayList<Pair<Integer, Location>> trajectory;
    private int startTime;

    @JSONField(serialize=false, deserialize=false)
    private int pathIndex;
    @JSONField(serialize=false, deserialize=false)
    private Location rackLocation;
    @JSONField(serialize=false, deserialize=false)
    private Location pickerLocation;
 
    public Path(ArrayList<Pair<Integer, Location>> trajectory, int startTime) {
        this.trajectory = trajectory;
        this.startTime = startTime;
        this.pathIndex = 0;
    }

    @JSONField(name="s")
    public int getStartTime() {
        return this.startTime;
    }

    @JSONField(name="s")
    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }

    @JSONField(name="t")
    public ArrayList<Pair<Integer, Location>> getTrajectory() {
        return this.trajectory;
    }

    @JSONField(name="t")
    public void setTrajectory(ArrayList<Pair<Integer, Location>> traj) {
        this.trajectory = traj;
    }

    public void setRackLocation(Location rackLocation) {
        this.rackLocation = rackLocation;
    }

    public void setPickerLocation(Location pickerLocation) {
        this.pickerLocation = pickerLocation;
    }

    // public void setIndex() {
    //     this.pathIndex = 0;
    // }

    // public int getIndex() {
    //     return this.pathIndex;
    // }

    public Location nextLocation(int currentTime) {
        if (pathIndex >= trajectory.size()) return null;
        int duration = currentTime - this.startTime;
        if (duration >= this.trajectory.get(this.pathIndex).getKey()) {
            ++pathIndex;
            return this.trajectory.get(this.pathIndex - 1).getValue();
        }
        return null;
    }

    public Location currentLocation(int currentTime) {
        int duration = currentTime - this.startTime;
        if (this.pathIndex >= this.trajectory.size()) 
            return this.trajectory.get(this.trajectory.size() - 1).getValue();
        int old = this.trajectory.get(this.pathIndex==0?0:(this.pathIndex - 1)).getKey();
        Location oldLocation = this.trajectory.get(this.pathIndex==0?0:(this.pathIndex - 1)).getValue();
        int delta = duration - old;
        Location newLocation = this.trajectory.get(this.pathIndex).getValue();
        if (newLocation.getC() == oldLocation.getC()) {
            int dir = newLocation.getR() - oldLocation.getR() > 0?1:-1;
            return new Location(oldLocation.getR() + dir * delta, oldLocation.getC());
        } 
        // R equals
        int dir = newLocation.getC() - oldLocation.getC() > 0?1:-1;
        return new Location(oldLocation.getR(), oldLocation.getC() + dir * delta);
    }

    @JSONField(serialize=false)
    public boolean isLocatedRack() {
        if (this.pathIndex == 0) return false;
        return this.trajectory.get(this.pathIndex - 1).getValue().equals(this.rackLocation);
    }

    @JSONField(serialize=false)
    public boolean isLocatedPicker() {
        return this.trajectory.get(this.pathIndex - 1).getValue().equals(this.pickerLocation);
    }

    public void extend(Path p) {
        int timeOffset = this.trajectory.get(this.trajectory.size() - 1).getKey();
        this.trajectory.remove(this.trajectory.size() - 1);
        for (Pair<Integer, Location> stp: p.getTrajectory()) {
            this.trajectory.add(Pair.of(timeOffset + stp.getKey(), stp.getValue()));
        }
    }

    @JSONField(serialize=false)
    public int getFinishTime() {
        return this.startTime + this.trajectory.get(this.trajectory.size() - 1).getKey();
    }
}
