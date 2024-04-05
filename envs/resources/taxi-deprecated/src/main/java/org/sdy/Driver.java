package org.sdy;

import com.opencsv.CSVReader;
import org.apache.commons.lang3.tuple.Pair;

import java.io.FileReader;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Double.valueOf;

enum State {
    OFF, IDLE, REPO, SERVE
}

public class Driver {
    private static int globalNum = 0;
    private static Map<String, Pair<Double, Double>[]> repoRoutes = loadRoutes();
    private double lng;
    private double lat;
    private int id;
    private State state;
    private String gridId;
    private int timer;
    private int t2;
    private int p;
    private Pair<Double, Double>[] track;
    private String serve_order;
    private int serve_state;
    private boolean is_assign;

    private static Map<String, Pair<Double, Double>[]> loadRoutes() {
        try {
            CSVReader reader = new CSVReader(
                    new FileReader(Paths.get(GlobalSetting.dataRoot.toString(), "repo_route.csv").toString())
            );
            String[] line;
            Map<String, Pair<Double, Double>[]> routes = new HashMap<>();
            while((line = reader.readNext()) != null) {
                String key = line[0];
                int len = (line.length - 1) / 2;
                Pair<Double, Double>[] value = new Pair[len];
                for (int i = 0; i < len; i++) {
                    value[i] = Pair.of(valueOf(line[2 * i + 1]), valueOf(line[2 * i + 2]));
                }
                routes.put(key, value);
            }
            return routes;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Driver(String gridId, int currentSecond) {
        this.gridId = gridId;
        this.timer = 0;
        this.t2 = 0;
        this.p = 0;
        this.track = null;
        this.state = State.IDLE;
        this.id = globalNum++;

        double[] lngLat = Geo.genRandom(gridId);
        this.lng = lngLat[0];
        this.lat = lngLat[1];
        this.serve_order = "";
        this.serve_state = -1;
        is_assign = true;
    }

    public int getId() {
        return this.id;
    }

    public void repoTo(String gridId) {
        this.state = State.REPO;
        this.timer = 0;
        this.p = 1;
        String routeKey = this.gridId + "_" + gridId;
        if (Driver.repoRoutes.containsKey(routeKey)) {
            this.track = Driver.repoRoutes.get(routeKey);
        } else {
            double[] fLngLat = Geo.getGridLocation(gridId);
            this.track = new Pair[2];
            this.track[0] = Pair.of(this.lng, this.lat);
            this.track[1] = Pair.of(fLngLat[0], fLngLat[1]);
        }
        this.t2 = (int)(Geo.distance(this.track[p - 1], this.track[p], 'a') / GlobalSetting.SPEED);
        if(!GlobalSetting.IS_REPO_CAN_SERVE) {
            this.is_assign = false;
        }
    }

    public void assignOrder(Order order) {
        this.state = State.SERVE;
        this.timer = 0;
        this.t2 = order.getDuration();
        this.track = new Pair[1];
        this.p = 0;
        this.track[0] = Pair.of(order.getEndLng(), order.getEndLat());
        this.serve_state = 0;
        this.serve_order = order.getOrderId();
        this.is_assign = false;
    }

    public boolean isAvail() {
        return this.state == State.IDLE || this.state == State.REPO;
    }

    public boolean isRepo() {
        return (this.state == State.IDLE || this.state == State.REPO)
                && this.timer % GlobalSetting.REPO_FREQUENCY == 0;
    }

    public String getGridId() {
        return this.gridId;
    }

    public void update(int currentSecond) {
        this.timer += 2;
        switch (this.state) {
            case IDLE:
                // for IDLE driver, timer is the only useful value
                if (this.timer % GlobalSetting.IDLE_TRANSITION_FREQUENCY == 0) {
                    this.gridId = DriverModel.transToNext(currentSecond, this.gridId);
                    double[] lngLat = Geo.genRandom(this.gridId);
                    this.lng = lngLat[0];
                    this.lat = lngLat[1];
                    this.timer = 0;
                } else if (this.timer % GlobalSetting.IDLE_UPDATE_FREQUENCY == 0) {
                    double[] lngLat = Geo.genRandom(this.gridId);
                    this.lng = lngLat[0];
                    this.lat = lngLat[1];
                    this.timer = 0;
                }
                break;
            case REPO:
                // for REPO driver, p, timer and t2 is useful
                if (this.timer % GlobalSetting.REPO_UPDATE_FREQUENCY == 0) {
                    if (this.timer < this.t2) {
                        double k = (double)this.timer / this.t2;
                        this.lng = (1 - k) * this.track[this.p - 1].getLeft() + k * this.track[this.p].getLeft();
                        this.lat = (1 - k) * this.track[this.p - 1].getRight() + k * this.track[this.p].getRight();
                        this.gridId = Geo.findGrid(this.lng, this.lat, this.gridId);
                    } else {
                        this.p += 1;
                        if (this.p >= this.track.length) {
                            this.state = State.IDLE;
                            this.timer = 0;
                            this.track = null;
                            this.p = 0;
                            this.t2 = 0;
                            this.is_assign = true;
                        } else {
                            this.timer = 0;
                            this.lng = this.track[p - 1].getLeft();
                            this.lat = this.track[p - 1].getRight();
                            this.gridId = Geo.findGrid(this.lng, this.lat, this.gridId);
                            this.t2 = (int)(Geo.distance(this.track[p - 1], this.track[p], 'a') / GlobalSetting.SPEED);
                        }
                    }
                }
                break;
            case SERVE:
                if (this.timer >= this.t2) {
                    this.state = State.IDLE;
                    this.timer = 0;
                    this.lng = this.track[0].getLeft();
                    this.lat = this.track[0].getRight();
                    this.gridId = Geo.findGrid(this.lng, this.lat);
                    this.track = null;
                    this.p = 0;
                    this.t2 = 0;
                    this.serve_state = 2;
                    this.is_assign = true;
                }
                break;
            default:
                break;
        }
    }

    public double getLng() {
        return this.lng;
    }

    public double getLat() {
        return this.lat;
    }

    public double[] getLocation() {
       double[] loc = new double[2];
       loc[0] = this.lng;
       loc[1] = this.lat;
       return loc;
    }

    public boolean is_assign() {
        return this.is_assign;
    }

    public static class TrackRecord {
        private final int driverId;
        private final String orderId;
        private final int duration;

        TrackRecord(int driverId, String orderId, int duration) {
            this.driverId = driverId;
            this.orderId = orderId;
            this.duration = duration;
        }

        public int getDriverId() {
            return driverId;
        }

        public int getDuration() {
            return duration;
        }

        public String getOrderId() {
            return orderId;
        }
    }

    public TrackRecord getTrackRecord() {
        if(this.serve_state == 0) {
           this.serve_state = 1;
           return new TrackRecord(this.id, this.serve_order, this.t2);
        } else if(this.serve_state == 2) {
            this.serve_state = -1;
            return new TrackRecord(this.id, this.serve_order, 0);
        }
        return null;
    }

    public static void main(String[] args) {
        System.out.println(1);
        return;
    }



}
