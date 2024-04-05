package SimulatorCore;
import java.util.Objects;

public class Task implements Cloneable{
    private int id;
    private int timestamp;
    private int rackId;
    private int pickingTime;

    public Task(int id, int rackId, int pickingTime, int timestamp) {
        this.id = id;
        this.rackId = rackId;
        this.pickingTime = pickingTime;
        this.timestamp = timestamp;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
    public int getRackId() {
        return this.rackId;
    }

    public void setAll(int id, int rackId, int pickingTime, int timestamp) {
        this.id = id;
        this.rackId = rackId;
        this.pickingTime = pickingTime;
        this.timestamp = timestamp;
    }

    public int getId() {
        return this.id;
    }

    public int getTimestamp() {
        return this.timestamp;
    }

    public int getPickingTime() {
        return this.pickingTime;
    }

    public boolean isFinish(int hasPicking) {
        return hasPicking >= pickingTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return id == task.id && timestamp == task.timestamp && rackId == task.rackId && pickingTime == task.pickingTime;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, timestamp, rackId, pickingTime);
    }
}
