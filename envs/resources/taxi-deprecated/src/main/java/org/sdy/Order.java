package org.sdy;

import java.util.Date;

public class Order implements Cloneable {
    private String orderId;
    private int beginTimestamp;
    private int endTimestamp;
    private double beginLng;
    private double beginLat;
    private double endLng;
    private double endLat;
    private double reward;
    private int startSecond;
    private double[] cancelProb;
    private double[] startLocation;
    private double[] finishLocation;

    public Order(String orderId, int beginTimestamp, int endTimestamp, double beginLng, double beginLat,
                 double endLng, double endLat, double reward, double[] cancelProb) {
            setAll(orderId, beginTimestamp, endTimestamp, beginLng, beginLat, endLng, endLat, reward, cancelProb);
    }

    public Order clone() {
        Object obj = null;
        try {
            obj = super.clone();
            return (Order) obj;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    public void setAll(String orderId, int beginTimestamp, int endTimestamp, double beginLng, double beginLat, double endLng, double endLat, double reward, double[] cancelProb) {
        this.orderId = orderId;
        this.beginTimestamp = beginTimestamp;
        this.endTimestamp = endTimestamp;
        this.beginLng = beginLng;
        this.beginLat = beginLat;
        this.endLng = endLng;
        this.endLat = endLat;
        this.reward = reward;
        this.cancelProb = cancelProb;
        Date date = new Date((long)this.beginTimestamp * 1000);
        this.startSecond = date.getHours() * 3600 + date.getMinutes() * 60 + date.getSeconds();
        this.startLocation = new double[2];
        this.startLocation[0] = this.beginLng;
        this.startLocation[1] = this.beginLat;
        this.finishLocation = new double[2];
        this.finishLocation[0] = this.endLng;
        this.finishLocation[1] = this.endLat;
    }

    public String getOrderId() {
        return orderId;
    }

    public int getBeginTimestamp() {
        return beginTimestamp;
    }

    public int getDuration() {
        return endTimestamp - beginTimestamp;
    }

    public int getStartSecond() {
        return startSecond;
    }

    public int getEndTimestamp() {
        return endTimestamp;
    }

    public double getBeginLat() {
        return beginLat;
    }

    public double getBeginLng() {
        return beginLng;
    }

    public double getEndLng() {
        return endLng;
    }

    public double getEndLat() {
        return endLat;
    }

    public double getReward() {
        return reward;
    }

    public boolean cancel(int distance) {
        return Math.random() < this.cancelProb[Math.min(Math.max(0, distance / 200), 9)];
    }

    public boolean cancel(double lng, double lat) {
        int distance = (int)Geo.distance(this.beginLng, this.beginLat, lng, lat);
        return cancel(distance);
    }

    public double[] getStartLocation() {
        return this.startLocation;
    }

    public double[] getFinishLocation() {
        return this.finishLocation;
    }

}
