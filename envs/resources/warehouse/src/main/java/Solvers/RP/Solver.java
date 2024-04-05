package Solvers.RP;

import SimulatorCore.*;

import static Solvers.RP.SpatioTemporalGraph.AStar;
import org.apache.commons.lang3.tuple.Pair;
import java.util.*;

public class Solver implements Service {

    private SpatioTemporalGraph SPG;
    public Solver() { }

    @Override
    public void initialize(PickerGroup pg, RackGroup rg) {
        this.SPG = new SpatioTemporalGraph(Ground.INSTANCE.getHeight(), Ground.INSTANCE.getWidth(), rg);
    }

    @Override
    public HashMap<Integer, Integer> assign(int currentTime, ArrayList<Robot> availRobot,
                                            HashMap<Integer, Task> availTasks) {
        this.SPG.update(currentTime);
        // distance greedy for most simple method
        HashMap<Integer, Integer> assignment = new HashMap<>();
        // calculate distance
        HashSet<Integer> assignedRobotId = new HashSet<>();
        HashSet<Integer> assignedTaskId = new HashSet<>();
        List<Pair<Pair<Integer, Integer>, Integer>> pairs = new ArrayList<>();
        // sort based on manhattan distance
        for (Robot r: availRobot) {
            Location robotLocation = r.getLocation();
            for (Task t: availTasks.values()) {
                pairs.add(Pair.of(Pair.of(r.getRobotId(), t.getId()),
                        robotLocation.manhattanDistance(Convertor.taskToRack(t).getLocation())));
            }
        }
        pairs.sort(Comparator.comparingInt(Pair::getValue));
        // greedy assign
        for (Pair<Pair<Integer, Integer>, Integer> p: pairs) {
            int robotId = p.getKey().getKey();
            int taskId = p.getKey().getValue();
            if (!assignedRobotId.contains(robotId) && !assignedTaskId.contains(taskId)) {
                assignment.put(taskId, robotId);
                assignedRobotId.add(robotId);
                assignedTaskId.add(taskId);
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
                    new int[]{rack.getLocation().getR(), rack.getLocation().getC()}, SPG);
            Path rackToPicker = AStar(currentTime + robotToRack.getFinishTime() + 1,
                    new int[]{rack.getLocation().getR(), rack.getLocation().getC()},
                    new int[]{p.getLocation().getR(), p.getLocation().getC()}, SPG);
            robotToRack.extend(rackToPicker);
            SPG.insertSinglePath(robotToRack);
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
                    new int[]{target.getR(), target.getC()}, SPG);
            robotIdToPath.put(r.getRobotId(), p);
            SPG.insertSinglePath(p);

        }
        return robotIdToPath;
    }

    @Override
    public Object getIndex() {
        return this.SPG;
    }

}
