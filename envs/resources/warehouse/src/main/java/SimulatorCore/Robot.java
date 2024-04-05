package SimulatorCore;

enum RobotState {
    IDLING, PICKING_UP, DELIVERING, QUEUING, PICKING, RETURNING
}

public class Robot {
    private static int robotIdGen = 0;
    private final int robotId;
    private Location location;
    private RobotState state;
    private Task assigned;
    private Path path;
    private int idleTime;
    private int movingDistance;
    private int carriedMovingDistance;
    private int queuingTime;
    private int pickingTime;

    public int getCarriedMovingDistance() {
        return this.carriedMovingDistance;
    }

    public int getEmptyMovingDistance() {
        return this.movingDistance - this.carriedMovingDistance;
    }

    public int getIdleTime() {
        return this.idleTime;
    }

    public Robot(Location initLocation) {
        this.robotId = robotIdGen++;
        this.location = initLocation;
        this.state = RobotState.IDLING;
        this.assigned = null;
        this.path = null;
        this.idleTime = 0;
        this.movingDistance = 0;
        this.queuingTime = 0;
        this.carriedMovingDistance = 0;
        this.pickingTime = 0;
    }

    public Location getLocation() {
        return this.location;
    }

    public int getRobotId() {
        return this.robotId;
    }

    public boolean isIdling() {
        return this.state == RobotState.IDLING;
    }

    public Task getAssigned() {
        return this.assigned;
    }

    public boolean update(int t) {
        boolean isChange = true;
        if (this.state == RobotState.IDLING) {
            isChange = false;
            ++this.idleTime;
        } else if (this.state == RobotState.PICKING_UP) {
            Location newLoc = path.nextLocation(t);
            if (newLoc != null) {
                this.movingDistance += this.location.manhattanDistance(newLoc);
                Convertor.taskToRack(this.assigned).updatePickUpTime(this.location.manhattanDistance(newLoc));
                this.location = newLoc;
            }
            if (path.isLocatedRack()) {
                this.state = RobotState.DELIVERING;
            }
        } else if (this.state == RobotState.DELIVERING) {
            int deltaDis = updateLocation(t);
            Convertor.taskToRack(this.assigned).updateDeliveryTime(deltaDis);
            if (path.isLocatedPicker()) {
                this.state = RobotState.QUEUING;
                Picker p = Convertor.robotToPicker(this);
                p.askForQueue(this);
            }
        } else if (this.state == RobotState.QUEUING) {
            ++this.queuingTime;
            Convertor.taskToRack(this.assigned).updateQueueTime();
        } else if (this.state == RobotState.PICKING) {
            ++this.pickingTime;
            Convertor.taskToRack(this.assigned).updateProcessingTime();
        } else if (this.state == RobotState.RETURNING) {
            int deltaDis = updateLocation(t);
            Convertor.taskToRack(this.assigned).updateReturnTime(deltaDis);
            if (path.isLocatedRack()) {
                RackGroup.INSTANCE.updateAvail(this.assigned.getRackId());
                this.state = RobotState.IDLING;
                this.assigned = null;
                this.path = null;
            }
        }
        return isChange;
    }

    private int updateLocation(int t) {
        Location newLoc = path.nextLocation(t);
        int deltaDis = 0;
        if (newLoc != null) {
            this.movingDistance += this.location.manhattanDistance(newLoc);
            this.carriedMovingDistance += this.location.manhattanDistance(newLoc);
            deltaDis = this.location.manhattanDistance(newLoc);
            this.location = newLoc;
        }
        return deltaDis;
    }

    public Location currentLocation(int t) {
        if (this.path == null) {
            return this.location;
        }
        return path.currentLocation(t);
    }

    public void beginPicking() {
        this.state = RobotState.PICKING;
    }

    public void finishPicking() {
        this.state = RobotState.RETURNING;
        this.path = null;
    }

    public boolean isReadyToReturn() {
        return this.state == RobotState.RETURNING && this.path == null;
    }

    public void dispatch(int currentTime, Task task, Path path) {
        this.path = path;
        this.assigned = task;
        this.state = RobotState.PICKING_UP;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public String statistics(String prefix) {
        return prefix + String.format("%d-%d-%d-%d-%d", robotId, idleTime, movingDistance, queuingTime, carriedMovingDistance);
    }

    public double getSingleWorkingRate(int currentTime) {
        return (double)this.pickingTime / currentTime;
    }

    public double getSingleQueuingTime() {
        return this.queuingTime;
    }

    public boolean carried() {
        return this.assigned != null && this.state != RobotState.PICKING_UP;
    }

}
