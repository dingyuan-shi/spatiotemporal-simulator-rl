package Solvers.GMAPF;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import SimulatorCore.Convertor;
import SimulatorCore.Location;
import SimulatorCore.Path;
import SimulatorCore.Picker;
import SimulatorCore.PickerGroup;
import SimulatorCore.Rack;
import SimulatorCore.RackGroup;
import SimulatorCore.Robot;
import SimulatorCore.RobotGroup;
import SimulatorCore.Service;
import SimulatorCore.Task;
import Solvers.GMAPF.Graph.Graph;

public class Solver implements Service {

    private HashMap<Integer, Set<Integer>> pickerIdToRackIds;
    private Graph graph;

    public Solver(String layoutFileName) { 
        try {
            graph = new Graph(layoutFileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(PickerGroup pg, RackGroup rg) {
        int pickerNum = pg.getPickerNum();
        int rackNum = rg.getRackNum();
        pickerIdToRackIds = new HashMap<>();
        for (int i = 0; i < pickerNum; ++i) {
            pickerIdToRackIds.put(i, new HashSet<>());
        }
        for (int i = 0; i < rackNum; ++i) {
            pickerIdToRackIds.get(Convertor.rackToPickerId(i)).add(i);
        }
    }
        
    @Override
    public Object getIndex() {
        return null;
    }

    @Override
    public HashMap<Integer, Integer> assign(int currentTime, ArrayList<Robot> availRobot, HashMap<Integer, Task> availTasks) {
        // find all availPickers
        ArrayList<Integer> availPickers = (ArrayList<Integer>) availTasks.keySet().stream()
                .map(Convertor::rackToPickerId)
                .distinct().collect(Collectors.toList());
        availPickers.sort(Comparator.comparingInt(p -> Convertor.getPickerById(p).getFinishTime()));
        // assign
        HashMap<Integer, Integer> assignment = new HashMap<>();
        Set<Integer> assignedRobot = new HashSet<>();
        for (int pickerId: availPickers) {
            int cnt = 0;
            for (Integer taskId : pickerIdToRackIds.get(pickerId)) {
                Location taskLocation = Convertor.getRackById(taskId).getLocation();
                if (!availTasks.containsKey(taskId)) continue;
                int recMinDis = -1;
                int recRobot = -1;
                for (Robot r : availRobot) {
                    if (!assignedRobot.contains(r.getRobotId())) {
                        int currentDistance = r.getLocation().manhattanDistance(taskLocation);
                        if (recMinDis == -1 || recMinDis > currentDistance) {
                            recMinDis = currentDistance;
                            recRobot = r.getRobotId();
                        }
                    }
                }
                if (recRobot == -1) break;
                assignedRobot.add(recRobot);
                assignment.put(taskId, recRobot);
                cnt++;
                if (cnt == 1) break;
            }
        }
        return assignment;
    }

    @Override
    public HashMap<Integer, Path> planning(int currentTime, HashMap<Integer, Integer> assignment, HashMap<Integer, Task> availTasks) {
        HashMap<Integer, Path> robotIdToPath = new HashMap<>();
        for (Integer taskId: assignment.keySet()) {
            int robotId = assignment.get(taskId);
            Robot robot = RobotGroup.INSTANCE.getRobotById(robotId);
            Task task = availTasks.get(taskId);
            Rack rack = RackGroup.INSTANCE.getRackById(task.getRackId());
            Picker picker = Convertor.rackToPicker(rack);
            Path pickUpPath = pathFinding(currentTime, robot.getLocation(), rack.getLocation());
            Path deliveryPath = pathFinding(currentTime, rack.getLocation(), picker.getLocation());
            pickUpPath.extend(deliveryPath);
            robotIdToPath.put(robotId, pickUpPath);
        }
        return robotIdToPath;
    };

    @Override
    public HashMap<Integer, Path> planning(int currentTime, ArrayList<Robot> readyReturnRobot) {
        HashMap<Integer, Path> robotIdToPath = new HashMap<>();
        for (Robot r: readyReturnRobot) {
            Location target = Convertor.robotToRack(r).getLocation();
            robotIdToPath.put(r.getRobotId(), pathFinding(currentTime, r.getLocation(), target));
        }
        return robotIdToPath;
    };
    
    private Path pathFinding(int currentTime, Location src, Location dst) {
        return graph.planning(currentTime, new Solvers.GMAPF.Graph.Location(src.getR(), src.getC()), new Solvers.GMAPF.Graph.Location(dst.getR(), dst.getC()));
    }
}
