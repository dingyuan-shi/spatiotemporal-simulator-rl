package org.sdy;

import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.concurrent.*;

public class DriverGroup {
    private HashMap<Integer, Driver> drivers;
    private HashSet<String> real_match;
    private int initSecond;
    private ExecutorService executorService = Executors.newFixedThreadPool(GlobalSetting.CORE_NUM);

    class updateThread extends Thread {
        private int split, numWork;
        private int currentSecond;
        private CountDownLatch countDownLatch;
        updateThread(CountDownLatch countDownLatch, int currentSecond, int split, int numWork) {
            this.currentSecond = currentSecond;
            this.split = split;
            this.countDownLatch = countDownLatch;
            this.numWork = numWork;
        }
        public void run () {
            for (Integer driverId: drivers.keySet()) {
                if (driverId % numWork == split) {
                    drivers.get(driverId).update(currentSecond);
                }
            }
            countDownLatch.countDown();
        }
    }

    class buildAvailThread extends Thread {
        private CountDownLatch countDownLatch;
        private final ArrayList<Driver>[] gridToDrivers;
        private int split, numWork;
        private HashMap<Integer, Driver> ownDrivers;
        buildAvailThread(CountDownLatch countDownLatch, ArrayList<Driver>[] gridToDrivers, HashMap<Integer, Driver> drivers, int split, int numWork) {
            this.countDownLatch = countDownLatch;
            this.gridToDrivers = gridToDrivers;
            this.split = split;
            this.numWork = numWork;
            this.ownDrivers = drivers;
        }

        public void run () {
            for (Integer driverId: this.ownDrivers.keySet()) {
                String gridId = this.ownDrivers.get(driverId).getGridId();
                if (gridId.charAt(0) % numWork == split) {
                    Driver driver = this.ownDrivers.get(driverId);
                    if (driver.isAvail()) {
                        int gridNum = Geo.getGridNum(gridId);
                        if (gridToDrivers[gridNum] == null) {
                            gridToDrivers[gridNum] = new ArrayList<>();
                        }
                        gridToDrivers[gridNum].add(driver);
                    }
                }
            }
            countDownLatch.countDown();
        }
    }

    public DriverGroup() {
        this.initSecond = 0;
        this.drivers = new HashMap<>();
        this.real_match = new HashSet<>();
    }

    public void initDrivers(int currentSecond) {
        this.initSecond = currentSecond;
        ArrayList<Driver> newDrivers = DriverModel.logOn(currentSecond);
        for (Driver d: newDrivers) {
            this.drivers.put(d.getId(), d);
        }
    }

    public void logOff(int currentSecond) {
        if (currentSecond == this.initSecond || currentSecond % GlobalSetting.UPDATE_DRIVER_LOG_OFF != 0) return;
        HashSet<Integer> removeKey = new HashSet<>();
        for (Integer driverId: this.drivers.keySet()) {
            Driver driver = this.drivers.get(driverId);
            if (DriverModel.logOff(currentSecond, driver)) {
                removeKey.add(driverId);
            }
        }
        for (Integer driverId: removeKey) {
            drivers.remove(driverId);
        }
    }

    public void logOn(int currentSecond) {
        if (currentSecond == this.initSecond || currentSecond % GlobalSetting.UPDATE_DRIVER_LOG_ON != 0) return;
        ArrayList<Driver> newDrivers = DriverModel.logOn(currentSecond);
        for (Driver d: newDrivers) {
            this.drivers.put(d.getId(), d);
        }
    }

    public int totalDriverNum() {
        return this.drivers.size();
    }

    public int availDriverNum() {
        int availNum = 0;
        for (Driver d: this.drivers.values()) {
            if (d.isAvail()) availNum++;
        }
        return availNum;
    }


    public void terminate() {
        this.executorService.shutdown();
    }

    public Pair<Integer, Double> assign(ArrayList<Pair<String, Integer>> matching, HashMap<String, Order> orderIdToOrder) {
        int successNum = 0;
        double successReward = 0.;
        this.real_match = new HashSet<>();
        for (Pair<String, Integer> assignPair: matching) {
            Order order = orderIdToOrder.get(assignPair.getLeft());
            Driver driver = this.drivers.get(assignPair.getRight());
            if (!order.cancel(driver.getLng(), driver.getLat())) {
                successNum++;
                successReward += order.getReward();
                driver.assignOrder(order);
                this.real_match.add(order.getOrderId());
            }
        }
        return Pair.of(successNum, successReward);
    }

    public HashSet<String> getRealMatch() {
        return real_match;
    }

    public void update(int currentSecond) {
        Set<Integer> driverIds = drivers.keySet();
        if (!GlobalSetting.IS_UPDATE_CONCURRENT || driverIds.size() < GlobalSetting.UPDATE_CONCURRENT_THRESHOLD) {
            for (Integer driverId: drivers.keySet()) {
                drivers.get(driverId).update(currentSecond);
            }
        } else {
            int numWork = Math.min(drivers.size() / GlobalSetting.UPDATE_CONCURRENT_THRESHOLD + 1, GlobalSetting.CORE_NUM);
            CountDownLatch lock = new CountDownLatch(numWork);
            for (int i = 0; i < numWork; i++) {
                executorService.execute(new updateThread(lock, currentSecond, i, numWork));
            }
            try {
                lock.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public ArrayList<Driver>[] getAvailDrivers() {
        ArrayList<Driver>[] gridToDrivers = new ArrayList[Geo.getTotalGridNum()];
        if (!GlobalSetting.IS_GET_AVAIL_CONCURRENT || drivers.size() < GlobalSetting.GET_AVAIL_CONCURRENT_THRESHOLD) {
            for (Integer driverId : this.drivers.keySet()) {
                Driver driver = this.drivers.get(driverId);
                if (driver.isAvail()) {
                    int gridNum = Geo.getGridNum(driver.getGridId());
                    if (gridToDrivers[gridNum] == null) {
                        gridToDrivers[gridNum] = new ArrayList<>();
                    }
                    gridToDrivers[gridNum].add(driver);
                }
            }
        } else {
            int numWork = Math.min(drivers.size() / GlobalSetting.GET_AVAIL_CONCURRENT_THRESHOLD + 1, GlobalSetting.CORE_NUM);
            CountDownLatch lock = new CountDownLatch(numWork);
            for (int i = 0; i < numWork; i++) {
                executorService.execute(new buildAvailThread(lock, gridToDrivers, this.drivers, i, numWork));
            }
            try {
                lock.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return gridToDrivers;
    }

    private Pair<CountDownLatch, Set<Integer>[]> splitWork(int concurrentThreshold) {
        Set<Integer> driverIds = drivers.keySet();
        int numWork = Math.min(driverIds.size() / concurrentThreshold + 1, GlobalSetting.CORE_NUM);
        CountDownLatch countDownLatch = new CountDownLatch(numWork);
        Set<Integer>[] sets = new Set[numWork];
        for (int i = 0; i < numWork; i++) sets[i] = new HashSet<>();
        for (Integer driverId: driverIds) {
            sets[driverId % numWork].add(driverId);
        }
        return Pair.of(countDownLatch, sets);
    }

    public ArrayList<Pair<String, Integer>> getRepoDrivers() {
        ArrayList<Pair<String, Integer>> repoDrivers = new ArrayList<>();
        for (Driver driver: this.drivers.values()) {
            if (driver.isRepo()) {
                repoDrivers.add(Pair.of(driver.getGridId(), driver.getId()));
            }
        }
        return repoDrivers;
    }

    public void repo(ArrayList<Pair<String, Integer>> repoResults) {
        for (Pair<String, Integer> driverIdGridId: repoResults) {
            Driver driver = drivers.get(driverIdGridId.getRight());
            driver.repoTo(driverIdGridId.getLeft());
        }
    }

    // 用于可视化的数据
    public ArrayList<Driver.TrackRecord> getTrackRecords() {
        ArrayList<Driver.TrackRecord> tracks = new ArrayList<>();
        for(int driverId: this.drivers.keySet()){
            Driver driver = this.drivers.get(driverId);
            Driver.TrackRecord record = driver.getTrackRecord();
            if(record != null) {
                tracks.add(record);
            }
        }
        return tracks;
    }

    public ArrayList<double[]> getDriverHeatPoints() {
        ArrayList<double[]> heat_points = new ArrayList<>();
        for(int driverId: this.drivers.keySet()){
            Driver driver = this.drivers.get(driverId);
            if(driver.is_assign()) {
                heat_points.add(new double[]{driver.getLng(), driver.getLat()});
            }
        }
        return heat_points;
    }

}
