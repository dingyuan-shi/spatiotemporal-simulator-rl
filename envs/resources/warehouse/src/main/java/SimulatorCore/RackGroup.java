package SimulatorCore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.PriorityQueue;
import org.apache.commons.lang3.tuple.Pair;

public enum RackGroup {
    INSTANCE;
    private final HashMap<Integer, Rack> idToRack = Ground.INSTANCE.getIdToRack();
    private final boolean[] isAvail = new boolean[idToRack.size()];

    RackGroup() {
        Arrays.fill(isAvail, true);
    }
    public Rack getRackById(int rackId) {
        return idToRack.get(rackId);
    }

    public boolean isRackAvail(int rackId) {
        return isAvail[rackId];
    }

    public void updateNotAvail(int rackId) {
        if (!isAvail[rackId]) {
            System.out.println("25 in rackGroup, abnormal: " + rackId);
        }
        isAvail[rackId] = false;
    }

    public void updateAvail(int rackId) {
        if (isAvail[rackId]) {
            System.out.println("32 in rackGroup, abnormal: " + rackId);
        }
        isAvail[rackId] = true;
    }

    public int getRackNum() {
        return idToRack.size();
    }

    public double[] avgTime() {
        double[] avg = new double[5];
        for (Rack rack: idToRack.values()) {
            avg[0] += rack.getPickUpTime();
            avg[1] += rack.getDeliveryTime();
            avg[2] += rack.getQueueTime();
            avg[3] += rack.getProcessingTime();
            avg[4] += rack.getReturnTime();
        }
        return avg;
    }

    public double[] avgTime(int rackId) {
        Rack selectedRack = idToRack.get(10);
        double[] avg = new double[5];
        avg[0] = selectedRack.getPickUpTime();
        avg[1] = selectedRack.getDeliveryTime();
        avg[2] = selectedRack.getQueueTime();
        avg[3] = selectedRack.getProcessingTime();
        avg[4] = selectedRack.getReturnTime();
        return avg;
    }

    public int[][] top10Racks() {
        int[][] rackTimeBreakdown = new int[10][3];
        PriorityQueue<Pair<Integer, Integer>> top10 = new PriorityQueue<>((a,b) -> a.getRight() - b.getRight());
        for (Integer rackId: idToRack.keySet()) {
            Rack rack = idToRack.get(rackId);
            int totalTime = rack.getTotalTime();
            if (top10.size() < 10) {
                top10.add(Pair.of(rackId, totalTime));
            } else {
                if (top10.peek().getRight() < totalTime) {
                    top10.poll();
                    top10.add(Pair.of(rackId, totalTime));
                }
            }
        }
        for (int i = 0; i < 10; ++i) {
            Rack rack = idToRack.get(top10.poll().getLeft());
            rackTimeBreakdown[i][0] = rack.getDeliveryTime() + rack.getPickUpTime() + rack.getReturnTime();
            rackTimeBreakdown[i][1] = rack.getQueueTime();
            rackTimeBreakdown[i][2] = rack.getProcessingTime();
        }
        return rackTimeBreakdown;
    }

}
