package Solvers.SAP;

import SimulatorCore.*;

import static Solvers.SAP.NaiveSpatioTemporalGraph.AStar;

import java.util.*;
import java.util.stream.Collectors;

public class Solver implements Service {

    private NaiveSpatioTemporalGraph NSPG;

    private HashMap<Integer, Set<Integer>> pickerIdToRackIds;

    public Solver () { }

    @Override
    public void initialize(PickerGroup pg, RackGroup rg) {
        this.NSPG = new NaiveSpatioTemporalGraph(Ground.INSTANCE.getHeight(), Ground.INSTANCE.getWidth(), rg);
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
    public HashMap<Integer, Integer> assign(int currentTime, ArrayList<Robot> availRobot, HashMap<Integer, Task> availTasks) {
        this.NSPG.update(currentTime);
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
           Robot r = Convertor.getRobotById(robotId);
           Rack rack = Convertor.taskToRack(availTasks.get(taskId));
           Picker p = Convertor.rackToPicker(rack);
           Path robotToRack = AStar(currentTime,
                   new int[]{r.getLocation().getR(), r.getLocation().getC()},
                   new int[]{rack.getLocation().getR(), rack.getLocation().getC()}, this.NSPG);
           Path rackToPicker = AStar(currentTime + robotToRack.getFinishTime() + 1,
                   new int[]{rack.getLocation().getR(), rack.getLocation().getC()},
                   new int[]{p.getLocation().getR(), p.getLocation().getC()}, this.NSPG);
           robotToRack.extend(rackToPicker);
           this.NSPG.insertSinglePath(robotToRack);
           robotIdToPath.put(robotId, robotToRack);
       }
       return robotIdToPath;
   }

   @Override
   public HashMap<Integer, Path> planning(int currentTime, ArrayList<Robot> readyReturnRobot) {
       HashMap<Integer, Path> robotIdToPath = new HashMap<>();
       for (Robot r: readyReturnRobot) {
           Location target = Convertor.robotToRack(r).getLocation();
           Path p = AStar(currentTime,
                   new int[]{r.getLocation().getR(), r.getLocation().getC()},
                   new int[]{target.getR(), target.getC()}, this.NSPG);
           robotIdToPath.put(r.getRobotId(), p);
           this.NSPG.insertSinglePath(p);
       }
       return robotIdToPath;
   }

    @Override
    public Object getIndex() {
        return this.NSPG;
    }
}
