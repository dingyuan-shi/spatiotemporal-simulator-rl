package org.sdy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.alibaba.fastjson.JSON;

import org.apache.commons.lang3.tuple.Pair;

public class Agent {
    public ArrayList<Pair<String, Integer>> dispatch(ArrayList<ODPair> dispatchObservation) {
        
        ArrayList<Pair<String, Integer>> match = new ArrayList<>();
        Set<Integer> assignedDrivers = new HashSet<>();
        Set<String> assignedOrders = new HashSet<>();
        for (int i = 0; i < dispatchObservation.size(); ++i) {
            ODPair pair = dispatchObservation.get(i);
            int driverId = pair.getDriverId();
            String orderId = pair.getOrderId();
            if (!assignedDrivers.contains(driverId) && !assignedOrders.contains(orderId)) {
                match.add(Pair.of(orderId, driverId));
                assignedDrivers.add(driverId);
                assignedOrders.add(orderId);
            }
        }
        return match;
    }

    public ArrayList<Pair<String, Integer>> reposition(RepoObservation repoObservation) {
        ArrayList<Pair<String, Integer>> repoResult = new ArrayList<Pair<String, Integer>>();
        for (Pair<String, Integer> driverIdGridId: repoObservation.getGridIdDriverId()) {
            int driverId = driverIdGridId.getRight();
            String gridId = driverIdGridId.getLeft();
            String[] grids = org.sdy.Geo.findGridMore(gridId);
            repoResult.add(Pair.of(grids[(int)(Math.random() * 100) % grids.length], driverId));
        }
        return repoResult;
    }

    public static void main(String[] args) throws Exception {
        Agent agent = new Agent();
        Engine env = new Engine(20161106);
        Pair<ArrayList<Pair<String, Integer>>, ArrayList<Pair<String, Integer>>> action = null;
        Pair<Pair<ArrayList<ODPair>, RepoObservation>, Double> observation = null;
        while ((observation = env.step(action)) != null) {
            ArrayList<ODPair> dispatchObservation = observation.getLeft().getLeft();
            RepoObservation repoObservation = observation.getLeft().getRight();
            ArrayList<Pair<String, Integer>> matching = null;
            ArrayList<Pair<String, Integer>> repo = null;
            if (dispatchObservation != null) {
                matching = agent.dispatch(dispatchObservation);
            }
            if (repoObservation != null) {
                repo = agent.reposition(repoObservation);
            }
            action = Pair.of(matching, repo);
        }
    }
}
