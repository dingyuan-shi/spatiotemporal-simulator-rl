package org.sdy;

import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.alibaba.fastjson.JSON;

public class Engine {
    private Agent agent;
    private DriverGroup driverGroup;
    private OrderLoader orderLoader;
    private int date, dow, year, month, day;
    private HashMap<String, Double> statistics;
    private int currentSecond;
    private ODPair metaODPair = new ODPair("", 0, 0, null, null, null, 0, 0, 0, 0);

    // for the step style
    private HashMap<String, Order> orderIdToOrder;
    private int orderIdToOrderTime;
    
    public Engine(Agent agent) {
        this.agent = agent;
        this.driverGroup = null;
        this.orderLoader = null;
        this.date = -1;
        this.dow = -1;
        this.statistics = new HashMap<String, Double>() {
            {
                put("totalDriverNum", 0.);
                put("availDriverNum", 0.);
                put("orderNum", 0.);
                put("ansOrderNum", 0.);
                put("compOrderNum", 0.);
                put("matchingTime", 0.);
                put("repoTime", 0.);
                put("batchRewards", 0.);
                put("accuRewards", 0.);
            }};
    }

    public Engine(int date) {
        this.driverGroup = null;
        this.orderLoader = null;
        this.date = -1;
        this.dow = -1;
        this.statistics = new HashMap<String, Double>() {
            {
                put("totalDriverNum", 0.);
                put("availDriverNum", 0.);
                put("orderNum", 0.);
                put("ansOrderNum", 0.);
                put("compOrderNum", 0.);
                put("matchingTime", 0.);
                put("repoTime", 0.);
                put("batchRewards", 0.);
                put("accuRewards", 0.);
            }};
        // for step style
        this.orderIdToOrder = new HashMap<>();
        this.orderIdToOrderTime = GlobalSetting.startTime;
        this.updateDay(date);
    }

    public void updateDay(int date) {
        this.date = date;
        this.currentSecond = GlobalSetting.startTime;
        this.year = date / 10000;
        this.month = (this.date - this.year * 10000) / 100;
        this.day = this.date % 100;
        this.dow = day % 7;  // TODO: this is simplified
        //TODO: handle the file logic
        this.orderLoader = new OrderLoader(date, GlobalSetting.orderRoot);
        this.driverGroup = new DriverGroup();
        System.out.println("begin loading drivers");
        this.driverGroup.initDrivers(GlobalSetting.startTime);
        System.out.println("finish loading drivers");
        this.orderLoader.skipTo(GlobalSetting.startTime);
        System.out.println("finish loading startTime");
    }


    public String[] stepJSON(String macthingAction, String repoAction) throws IOException {
        // System.out.println("java" +  this.currentSecond);
        ArrayList<HashMap<String, Integer>> ma = (ArrayList) JSON.parseArray(macthingAction, HashMap.class);
        ArrayList<HashMap<String, Integer>> ra = (ArrayList) JSON.parseArray(repoAction, HashMap.class);
        ArrayList<Pair<String, Integer>> rma = null;
        if (ma != null) {
            rma = new ArrayList<>();
            for (HashMap<String, Integer> single: ma) {
                for (String key: single.keySet()) {
                    rma.add(Pair.of(key, single.get(key)));
                }
            }
        }
        ArrayList<Pair<String, Integer>> rra = null;
        if (ra != null) {
            rra = new ArrayList<>();
            for (HashMap<String, Integer> single: ra) {
                for (String key: single.keySet()) {
                    rra.add(Pair.of(key, single.get(key)));
                }
            }
        }
        // if (rma != null) {
        //     System.out.println("109:" + rma.size());
        // }
        Pair<Pair<ArrayList<ODPair>, RepoObservation>, Double> observation_reward = step(Pair.of(rma, rra));
        String[] observations = new String[3];
        observations[0] = observation_reward.getLeft() == null?"":JSON.toJSONString(observation_reward.getLeft().getLeft());
        observations[1] = observation_reward.getLeft() == null?"":JSON.toJSONString(observation_reward.getLeft().getRight());
        observations[2] = JSON.toJSONString(observation_reward.getRight());
        return observations;
    }


    public Pair<Pair<ArrayList<ODPair>, RepoObservation>, Double> step(Pair<ArrayList<Pair<String, Integer>>, ArrayList<Pair<String, Integer>>> action) throws IOException {
        // step 0 judge if finished
        // note that when equal endTime, still remain one tic to execute
        if (this.currentSecond > GlobalSetting.endTime + 2) {
            driverGroup.terminate();
            this.currentSecond += 2;
            return Pair.of(null, this.statistics.get("batchRewards"));
        }
        if (this.currentSecond - this.orderIdToOrderTime > 2) this.orderIdToOrder.clear();
        // step 1 update drivers for the actions, 
        if (action != null) {
            ArrayList<Pair<String, Integer>> matching = action.getLeft();
            ArrayList<Pair<String, Integer>> repoResults = action.getRight();
            // 执行上一步里来自agent的decision
            if (matching != null) {
                this.statistics.put("ansOrderNum", this.statistics.get("ansOrderNum") + matching.size());
                Pair<Integer, Double> successNumReward = this.driverGroup.assign(matching, this.orderIdToOrder);
                this.statistics.put("compOrderNum", this.statistics.get("compOrderNum") + successNumReward.getLeft());
                this.statistics.put("batchRewards", successNumReward.getRight());
                this.statistics.put("accuRewards", this.statistics.get("accuRewards") + successNumReward.getRight());
            }
            if (repoResults != null) {
                this.driverGroup.repo(repoResults);
            }
        }
        if (this.currentSecond != GlobalSetting.startTime) {
            this.driverGroup.update(this.currentSecond);
        }
        if (this.currentSecond % GlobalSetting.LOG_FREQUENCY == 0) {
            this.outputStatistics();
        }

        this.driverGroup.logOff(this.currentSecond);
        this.driverGroup.logOn(this.currentSecond);
        // step 2 load order and execute the dispatch algorithm
        List<Order> orders = this.orderLoader.getOrders(this.currentSecond);
        ArrayList<ODPair> odPairs = null;
        if (orders.size() > 0) {
            this.statistics.put("orderNum", this.statistics.get("orderNum") + orders.size());
            this.orderIdToOrder.clear();
            this.orderIdToOrderTime = this.currentSecond;
            for (Order order: orders) {
                orderIdToOrder.put(order.getOrderId(), order);
            }
            ArrayList<Driver>[] gridToDrivers = this.driverGroup.getAvailDrivers();
            odPairs = this.buildDispatchObservation(currentSecond, gridToDrivers, orders);
        }
        // step 3 build repo observation and execute repo algorithm
        ArrayList<Pair<String, Integer>> repoDrivers = this.driverGroup.getRepoDrivers();
        RepoObservation repoObservation = null;
        if (repoDrivers.size() > 0) {
            int timestamp = Utils.secondToTimestamp(currentSecond, year, month, day);
            repoObservation = new RepoObservation(timestamp, dow, repoDrivers);
        }
        // step 4 update all drivers
        this.currentSecond += 2;
        return Pair.of(Pair.of(odPairs, repoObservation), this.statistics.get("batchRewards"));
    }

    public void startSimulation() throws IOException {
        for (; this.currentSecond <= GlobalSetting.endTime; this.currentSecond += 2) {
            // step 1 update drivers
            this.driverGroup.logOff(this.currentSecond);
            this.driverGroup.logOn(this.currentSecond);
            // step 2 load order and execute the dispatch algorithm
            List<Order> orders = this.orderLoader.getOrders(this.currentSecond);
            if (orders.size() > 0) {
                this.statistics.put("orderNum", this.statistics.get("orderNum") + orders.size());
                HashMap<String, Order> orderIdToOrder = new HashMap<>();
                for (Order order: orders) {
                    orderIdToOrder.put(order.getOrderId(), order);
                }
                ArrayList<Driver>[] gridToDrivers = this.driverGroup.getAvailDrivers();
                ArrayList<ODPair> odPairs = this.buildDispatchObservation(this.currentSecond, gridToDrivers, orders);
                long t1 = System.nanoTime();
                ArrayList<Pair<String, Integer>> matching = this.agent.dispatch(odPairs);
                long duration = System.nanoTime() - t1;
                this.statistics.put("matchingTime", this.statistics.get("matchingTime") + duration / 1e9);
                this.statistics.put("ansOrderNum", this.statistics.get("ansOrderNum") + matching.size());
                Pair<Integer, Double> successNumReward = this.driverGroup.assign(matching, orderIdToOrder);
                this.statistics.put("compOrderNum", this.statistics.get("compOrderNum") + successNumReward.getLeft());
                this.statistics.put("batchRewards", this.statistics.get("batchRewards"));
                this.statistics.put("accuRewards", this.statistics.get("accuRewards") + successNumReward.getRight());
            }
            // step 3 build repo observation and execute repo algorithm
            ArrayList<Pair<String, Integer>> repoDrivers = this.driverGroup.getRepoDrivers();
            if (repoDrivers.size() > 0) {
                int timestamp = Utils.secondToTimestamp(this.currentSecond, year, month, day);
                RepoObservation repoObservation = new RepoObservation(timestamp, dow, repoDrivers);
                long t1 = System.nanoTime();
                ArrayList<Pair<String, Integer>> repoResults = this.agent.reposition(repoObservation);
                long duration = System.nanoTime() - t1;
                this.statistics.put("repoTime", this.statistics.get("repoTime") + duration / 1e9);
                driverGroup.repo(repoResults);
            }
            // step 4 update all drivers
            this.driverGroup.update(this.currentSecond);
            // step 5 output the statistics
            if (this.currentSecond % GlobalSetting.LOG_FREQUENCY == 0) {
                this.outputStatistics();
            }
        }
        driverGroup.terminate();
    }

    private void outputStatistics() {
        double ansRate, compRate;
        if (this.currentSecond == GlobalSetting.startTime) {
            ansRate = 0;
            compRate = 0;
            statistics.put("ansOrderNum", 0.);
            statistics.put("compOrderNum", 0.);
        } else {
            ansRate = (double) statistics.get("ansOrderNum") / statistics.get("orderNum");
            compRate = (double) statistics.get("compOrderNum") / statistics.get("orderNum");
            statistics.put("ansOrderNum", ansRate);
            statistics.put("compOrderNum", compRate);
        }
        this.statistics.put("totalDriverNum", (double)this.driverGroup.totalDriverNum());
        this.statistics.put("availDriverNum", (double)this.driverGroup.availDriverNum());
        System.out.printf("%s: # batch order: %d, # driver: %d, # avail driver: %d, " +
                        "ans rate: %.3f, comp rate: %.3f, " +
                        "matching time: %.3f, repo time: %.3f, " +
                        "batch reward: %.2f, accu reward: %.2f\n",
                Utils.secondToHuman(this.currentSecond), (int)Math.floor(statistics.get("orderNum")),
                this.driverGroup.totalDriverNum(), this.driverGroup.availDriverNum(), ansRate, compRate,
                this.statistics.get("matchingTime"), this.statistics.get("repoTime"),
                this.statistics.get("batchRewards"), this.statistics.get("accuRewards"));

        this.statistics.put("orderNum", 0.);
        this.statistics.put("ansOrderNum", 0.);
        this.statistics.put("compOrderNum", 0.);
        this.statistics.put("batchRewards", 0.);
        this.statistics.put("repoTime", 0.);
        this.statistics.put("matchingTime", 0.);
    }

    public ArrayList<ODPair> buildDispatchObservation(int currentSecond, ArrayList<Driver>[] gridToDrivers, List<Order> orders) {
        int timestamp = Utils.secondToTimestamp(currentSecond, year, month, day);
        ArrayList<ODPair> odPairs = new ArrayList<>();
        for (Order order: orders) {
            String orderGrid = Geo.findGrid(order.getBeginLng(), order.getBeginLat());
            String[] adjacentGrids = Geo.findGridMore(orderGrid);
            for (String grid: adjacentGrids) {
                ArrayList<Driver> drivers = gridToDrivers[Geo.getGridNum(grid)];
                if (drivers == null) continue;
                for (Driver driver: drivers) {
                    double dist = Geo.distance(order.getBeginLng(), order.getBeginLat(),
                            driver.getLng(), driver.getLat(), 'a');
                    if (dist > 3000) continue;
                    ODPair tmpPair = metaODPair.clone();
                    tmpPair.setAll(order.getOrderId(), driver.getId(),
                            dist, order.getStartLocation(), order.getFinishLocation(),
                            driver.getLocation(), timestamp, this.dow, order.getReward(),
                            (int)(dist / GlobalSetting.SPEED));
                    odPairs.add(tmpPair);
                }
            }
        }
        return odPairs;
    }

    public static void main(String[] args) throws IOException {
        long t0 = System.nanoTime();
        Agent agent = new Agent();
        Engine engine = new Engine(agent);
        engine.updateDay(20161106);
        long t1 = System.nanoTime();
        // engine.startSimulation();
        long t2 = System.nanoTime();
        System.out.printf("finish simulation in %.3f seconds, total consuming %.3f\n",
                (t2 - t1) / 1e9, (t2 - t0) / 1e9);
    }
}
