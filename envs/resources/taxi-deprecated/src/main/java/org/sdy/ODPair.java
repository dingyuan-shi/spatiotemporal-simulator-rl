package org.sdy;

public class ODPair implements Cloneable {
    private String orderId;
    private int driverId;
    private double orderDriverDistance;

    private double[] orderStartLocation;

    private double[] orderFinishLocation;

    private double[] driverLocation;

    private int timestamp;
    private int dayOfWeek;
    private double rewardUnits;
    private int pickUpETA;

    public ODPair(String orderId, int driverId, double orderDriverDistance, double[] orderStartLocation, double[] orderFinishLocation,
    double[] driverLocation, int timestamp, int dayOfWeek,
                  double rewardUnits, int pickUpETA) {
        setAll(orderId,  driverId,  orderDriverDistance, orderStartLocation, orderFinishLocation,driverLocation, timestamp, dayOfWeek, rewardUnits,  pickUpETA);
    }

    public void setAll(String orderId, int driverId, double orderDriverDistance,
    double[] orderStartLocation, double[] orderFinishLocation,
    double[] driverLocation, int timestamp, int dayOfWeek,
        double rewardUnits, int pickUpETA) {
        this.orderId = orderId;
        this.driverId = driverId;
        this.orderDriverDistance = orderDriverDistance;
        this.orderStartLocation = orderStartLocation;
        this.orderFinishLocation = orderFinishLocation;
        this.driverLocation = driverLocation;
        this.timestamp = timestamp;
        this.dayOfWeek = dayOfWeek;
        this.rewardUnits = rewardUnits;
        this.pickUpETA = pickUpETA;
    }

    public ODPair clone() {
        Object obj = null;
        try {
            obj = super.clone();
            return (ODPair) obj;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    public String getOrderId() {
        return orderId;
    }

    public int getDriverId() {
        return driverId;
    }

    public double getOrderDriverDistance() {
        return orderDriverDistance;
    }

    public double[] getOrderStartLocation() {
        return orderStartLocation;
    }

    public double[] getOrderFinishLocation() {
        return orderFinishLocation;
    }

    public double[] getDriverLocation() {
        return driverLocation;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public double getRewardUnits() {
        return rewardUnits;
    }

    public int getPickUpETA() {
        return pickUpETA;
    }
}
