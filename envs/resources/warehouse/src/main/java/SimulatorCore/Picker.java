package SimulatorCore;

import java.util.LinkedList;
import java.util.Queue;

enum PickerState {
    IDLING, PICKING
}

public class Picker {
    private static int pickerIdGen = 0;
    private final int pickerId;
    private final Location location;
    private final Queue<Robot> robots;
    private Robot currentRobot;
    private int timer;
    private PickerState state;
    private int idleTime;
    private int pickingTime;
    private int rackNumber;

    public Picker(Location initLocation) {
        this.pickerId = pickerIdGen++;
        this.location = initLocation;
        this.currentRobot = null;
        this.robots = new LinkedList<>();
        this.timer = 0;
        this.state = PickerState.IDLING;
        this.idleTime = 0;
        this.pickingTime = 0;
        this.rackNumber = 0;
    }

    public Location getLocation() {
        return this.location;
    }

    public int getPickerId() {
        return this.pickerId;
    }

    public boolean askForQueue(Robot robot) {
        if (this.robots.size() == 0 && currentRobot == null) {
            currentRobot = robot;
            currentRobot.beginPicking();
            this.state = PickerState.PICKING;
            return false;
        }
        this.robots.offer(robot);
        return true;
    }

    public boolean picking(int t) {
        boolean isChange = false;
        if (this.state == PickerState.IDLING) {
            ++this.idleTime;
        } else if (this.state == PickerState.PICKING) {
            isChange = true;
            ++this.pickingTime;
            ++this.timer;
            if (currentRobot.getAssigned().isFinish(this.timer)) {
                this.timer = 0;
                currentRobot.finishPicking();
                ++this.rackNumber;
                currentRobot = this.robots.poll();
                if (currentRobot == null) {
                    this.state = PickerState.IDLING;
                } else {
                    currentRobot.beginPicking();
                }
            }
        }
        return isChange;
    }

    public String statistics(String prefix) {
        return prefix + String.format("%d-%d-%d-%d", pickerId, idleTime, pickingTime, rackNumber);
    }

    public int getFinishTime() {
        int time = 0;
        for (Robot r: this.robots) time += r.getAssigned().getPickingTime();
        return time + (currentRobot == null?0: (currentRobot.getAssigned().getPickingTime() - this.timer));
    }

    public int getQueueLen() {
        return this.robots.size();
    }

    public double getSingleWorkingRate() {
        return (double)this.pickingTime / (this.pickingTime + this.idleTime);
    }
}
