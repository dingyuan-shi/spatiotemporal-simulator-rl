package SimulatorCore;

public class Rack {
    private static int rackIdGen = 0;
    private final int rackId;
    private final int priorRackId;
    private final int pickerId;
    private final Location location;
    private int deliveryTime;
    private int processingTime;
    private int pickUpTime;
    private int returnTime;
    private int queueTime;

    public Rack(Location location, int pickerId, int priorRackId) {
        this.rackId = rackIdGen++;
        this.location = location;
        this.pickerId = pickerId;
        this.priorRackId = priorRackId;
        this.deliveryTime = 0;
        this.processingTime = 0;
        this.pickUpTime = 0;
        this.returnTime = 0;
        this.queueTime = 0;
    }

    public int getPickerId() {
        return pickerId;
    }

    public int getPriorRackId() {
        return priorRackId;
    }

    public Location getLocation() {
        return this.location;
    }

    public int getRackId() {
        return this.rackId;
    }

    public void updateProcessingTime() {
        this.processingTime++;
    }

    public void updateDeliveryTime(int delta) {
        this.deliveryTime += delta;
    }

    public void updatePickUpTime(int delta) {
        this.pickUpTime += delta;
    }

    public void updateReturnTime(int delta) {
        this.returnTime += delta;
    }

    public void updateQueueTime() {
        this.queueTime++;
    }

    public int getDeliveryTime() {
        return this.deliveryTime;
    }

    public int getProcessingTime() {
        return this.processingTime;
    }

    public int getPickUpTime() {
        return this.pickUpTime;
    }

    public int getReturnTime() {
        return returnTime;
    }

    public int getQueueTime() {
        return queueTime;
    }

    public int getTotalTime() {
        return this.deliveryTime + this.pickUpTime + this.queueTime + this.returnTime;
    }
}
