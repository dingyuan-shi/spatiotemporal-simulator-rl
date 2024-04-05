package SimulatorCore;

import java.util.ArrayList;
import java.util.HashMap;
import com.opencsv.exceptions.CsvValidationException;

import UserConfig.Configurations;
import org.apache.commons.lang3.tuple.Pair;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import static SimulatorCore.ServerProxy.planningBack;
import static SimulatorCore.ServerProxy.planningTo;


public class Engine {

    private static int assignedTasks;
    private static int lastAssignedTasks;
    private static int t;
    private static boolean pickerChange;
    private static boolean robotChange;
    private static Snapshot snapshot;
    private static Pair<Pair<HashMap<Integer, Location>, HashMap<Integer, Task>>, Pair<ArrayList<Robot>,  ArrayList<Robot>>> observ;

    public static Pair<Pair<HashMap<Integer, Location>, HashMap<Integer, Task>>, Pair<ArrayList<Robot>,  ArrayList<Robot>>> getObserv() {
        return observ;
    }

    public static void reset(String[] args) throws CsvValidationException, NumberFormatException {
        Configurations.setConfigurations(args);
        System.setOut(Configurations.ps);
        // TaskPool.INSTANCE.setIgnorePickingTime(Configurations.IGNORE_PICKING_TIME);
        Configurations.service.initialize(PickerGroup.INSTANCE, RackGroup.INSTANCE);
        snapshot = new Snapshot();
        // simulate
        assignedTasks = 0;
        lastAssignedTasks = 0;        
        t = 0;
        pickerChange = PickerGroup.INSTANCE.update(t);
        HashMap<Integer, Location> pickerInfo = PickerGroup.INSTANCE.getPickerInfo();
        HashMap<Integer, Task> availTasks = TaskPool.INSTANCE.getTasks(t);
        ArrayList<Robot> readyReturnRobot = RobotGroup.INSTANCE.getReadyReturnRobot();
        ArrayList<Robot> availRobot = RobotGroup.INSTANCE.getAvailRobot();
        observ = Pair.of(Pair.of(pickerInfo, availTasks), Pair.of(readyReturnRobot, availRobot));
    }
    
    public static void reset(String dataset) throws CsvValidationException, NumberFormatException {
        String[] args = new String[3];
        args[0] = "";
        args[1] = dataset;
        args[2] = "";
        reset(args);
    }

    private static String[] observToString(boolean done) {
        String[] observJson = new String[4];
        if (done) {
            observJson[0] = "";
            return observJson;
        }
        observJson[0] = JSON.toJSONString(observ.getLeft().getLeft());
        observJson[1] = JSON.toJSONString(observ.getLeft().getRight());
        observJson[2] = JSON.toJSONString(observ.getRight().getLeft());
        observJson[3] = JSON.toJSONString(observ.getRight().getRight());
        return observJson;
    }

    public static String[] resetJSON(String dataset) throws CsvValidationException, NumberFormatException {
        reset(dataset);
        return observToString(false);
    }

    private static HashMap<Integer, Path> parsePaths(String robotIdToPathStr) {
        HashMap<Integer, Path> robotIdToPath = new HashMap<>();
        JSONObject jsonObj = JSON.parseObject(robotIdToPathStr);
        for (String key: jsonObj.keySet()) {
            JSONObject pathjson = (JSONObject) jsonObj.get(key);
            int startTime = pathjson.getInteger("s");
            ArrayList<Pair<Integer, Location>> traj = new ArrayList<>();
            JSONArray trajjson = JSON.parseArray(pathjson.get("t").toString());
            for (int i = 0; i < trajjson.size(); ++i) {
                JSONObject stLoc = (JSONObject) trajjson.get(i);
                int time = 0;
                Location loc = null;
                for (String k: stLoc.keySet()) {
                    time = Integer.parseInt(k);
                    loc = (Location) JSON.parseObject(stLoc.get(k).toString(), Location.class);
                    break;
                }
                traj.add(Pair.of(time, loc));
            }
            robotIdToPath.put(Integer.parseInt(key), new Path(traj, startTime));
        }
        return robotIdToPath;
    }

    public static String[] stepJSON(String returnRobotIdToPathStr, String assignmentStr, String robotIdToPathStr) throws CsvValidationException, NumberFormatException {
        HashMap<Integer, Path> returnRobotIdToPath = parsePaths(returnRobotIdToPathStr);
        HashMap<Integer, Integer> assignment = (HashMap) JSON.parseObject(assignmentStr, new TypeReference<HashMap<Integer, Integer>>() {});
        HashMap<Integer, Path> robotIdToPath = parsePaths(robotIdToPathStr);
        returnRobotIdToPath = planningBack(returnRobotIdToPath);
        robotIdToPath = planningTo(assignment, observ.getLeft().getRight(), robotIdToPath);
        boolean done = step(returnRobotIdToPath, assignment, robotIdToPath);
        return observToString(done);
    }

    public static boolean step(HashMap<Integer, Path> returnRobotIdToPath, HashMap<Integer, Integer> assignment, HashMap<Integer, Path> robotIdToPath) throws CsvValidationException, NumberFormatException {
        if (assignedTasks - lastAssignedTasks >= UserConfig.Configurations.LOG_SEG) {
            double pwr = PickerGroup.INSTANCE.getWorkingRate();
            double amr = RobotGroup.INSTANCE.getWorkingRate(t);
            System.out.printf("assigned %6d tasks, Makespan: %8d, PWR: %.3f, RWR: %.3f\n", assignedTasks, t, pwr, amr);
            lastAssignedTasks = assignedTasks;
        }
        RobotGroup.INSTANCE.dispatchReturn(returnRobotIdToPath);
        RobotGroup.INSTANCE.dispatchTask(t, assignment, robotIdToPath, observ.getLeft().getRight());
        assignedTasks += TaskPool.INSTANCE.removeAssigned(assignment);
        
        robotChange = RobotGroup.INSTANCE.update(t);
        pickerChange = PickerGroup.INSTANCE.update(t);
        // 都不动并且generator后面也没有了 就break
        boolean finish = false;
        if (!pickerChange && !robotChange && !TaskPool.INSTANCE.hasNext() && t != 0) { 
            double pwr = PickerGroup.INSTANCE.getWorkingRate();
            double amr = RobotGroup.INSTANCE.getWorkingRate(t);
            System.out.printf("assigned %6d tasks, Makespan: %8d, PWR: %.3f, RWR: %.3f\n", assignedTasks, t, pwr, amr);
            finish = true;
            
        }
        snapshot.dump(finish, t);
        HashMap<Integer, Location> pickerInfo = PickerGroup.INSTANCE.getPickerInfo();
        HashMap<Integer, Task> availTasks = TaskPool.INSTANCE.getTasks(t);
        ArrayList<Robot> readyReturnRobot = RobotGroup.INSTANCE.getReadyReturnRobot();
        ArrayList<Robot> availRobot = RobotGroup.INSTANCE.getAvailRobot();
        observ = Pair.of(Pair.of(pickerInfo, availTasks), Pair.of(readyReturnRobot, availRobot));
        ++t;
        return finish;
    }
    
}
