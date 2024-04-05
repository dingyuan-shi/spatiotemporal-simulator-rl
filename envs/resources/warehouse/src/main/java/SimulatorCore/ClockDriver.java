package SimulatorCore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import com.opencsv.exceptions.CsvValidationException;

import org.apache.lucene.util.RamUsageEstimator;

public class ClockDriver {
    public static HashMap<String, Object> simulate(ServerProxy sp) throws CsvValidationException, NumberFormatException {
        int assignedTasks = 0;
        int lastAssignedTasks = 0;
        long matchingUsage = 0;
        long planingUsage = 0;
        long totalUsage = 0;
        long startTime = System.currentTimeMillis();
        int t = 0;
        boolean pickerChange = PickerGroup.INSTANCE.update(t);
        while (true) {
            if (assignedTasks - lastAssignedTasks >= UserConfig.Configurations.LOG_SEG) {
                double pwr = PickerGroup.INSTANCE.getWorkingRate();
                double amr = RobotGroup.INSTANCE.getWorkingRate(t);
                long MC = RamUsageEstimator.sizeOf(sp);
                System.out.printf("assigned %6d tasks, Makespan: %8d, time use: %.3f, STC: %.3f, " +
                                "PTC: %.3f, PWR: %.3f, RWR: %.3f, MC: %f\n",
                        assignedTasks, t, totalUsage / 1000.0, matchingUsage / 1000.0,
                        planingUsage / 1000.0, pwr, amr, MC / 1024.0);
                lastAssignedTasks = assignedTasks;
            }
            // Step 1 planing for returning
            ArrayList<Robot> readyReturnRobot = RobotGroup.INSTANCE.getReadyReturnRobot();
            if (readyReturnRobot.size() > 0) {
                long begin = System.currentTimeMillis();
                HashMap<Integer, Path> robotIdToPath = sp.planning(t, readyReturnRobot);
                planingUsage += (System.currentTimeMillis() - begin);
                RobotGroup.INSTANCE.dispatchReturn(robotIdToPath);
            }
            // Step 2 get new tasks
            ArrayList<Robot> availRobot = RobotGroup.INSTANCE.getAvailRobot();
            if (availRobot.size() > 0) {
                HashMap<Integer, Task> availTasks = TaskPool.INSTANCE.getTasks(t);
                if (availTasks.size() > 0) {
                    long begin = System.currentTimeMillis();
                    HashMap<Integer, Integer> assignment = sp.assign(t, availRobot, availTasks);
                    checkLegality(t, assignment, availRobot, availTasks);
                    matchingUsage += System.currentTimeMillis() - begin;
                    begin = System.currentTimeMillis();
                    HashMap<Integer, Path> robotIdToPath = sp.planning(t, assignment, availTasks);
                    planingUsage += (System.currentTimeMillis() - begin);
                    // delete from TaskPool
                    RobotGroup.INSTANCE.dispatchTask(t, assignment, robotIdToPath, availTasks);
                    assignedTasks += TaskPool.INSTANCE.removeAssigned(assignment);
                }
            }
            totalUsage = (System.currentTimeMillis() - startTime);
            boolean robotChange = RobotGroup.INSTANCE.update(t);
            pickerChange = PickerGroup.INSTANCE.update(t);
            // 都不动并且generator后面也没有了 就break
            if (!pickerChange && !robotChange && !TaskPool.INSTANCE.hasNext() && t != 0) break;
            ++t;
        }
        double pwr = PickerGroup.INSTANCE.getWorkingRate();
        double amr = RobotGroup.INSTANCE.getWorkingRate(t);
        long MC = RamUsageEstimator.sizeOf(sp);
        System.out.printf("assigned %6d tasks, Makespan: %8d, time use: %.3f, STC: %.3f, " +
                        "PTC: %.3f, PWR: %.3f, RWR: %.3f, MC: %f\n",
                assignedTasks, t, totalUsage / 1000.0, matchingUsage / 1000.0, planingUsage / 1000.0, pwr, amr, MC / 1024.0);
        HashMap<String, Object> res = new HashMap<>();
        res.put("totalUsage", totalUsage);
        res.put("matchingUsage", matchingUsage);
        res.put("planningUsage", planingUsage);
        res.put("simulationStep", t);
        return res;
    }

    private static void checkLegality(int t, HashMap<Integer, Integer> assignment, ArrayList<Robot> availRobot, HashMap<Integer, Task> availTasks) {
        HashSet<Integer> assignedRobot = new HashSet<>();
        HashSet<Integer> availRobotSet = new HashSet<>();
        for (Robot r: availRobot) {
            availRobotSet.add(r.getRobotId());
        }
        for (Integer taskId: assignment.keySet()) {
            int robotId = assignment.get(taskId);
            if (!availRobotSet.contains(robotId) || assignedRobot.contains(robotId)) {
                System.out.println("error at " + t);
            }
            assignedRobot.add(robotId);
            if (!availTasks.containsKey(taskId) || !RackGroup.INSTANCE.isRackAvail(taskId)) {
                System.out.println("error at " + t);
            }
        }
    }
}
