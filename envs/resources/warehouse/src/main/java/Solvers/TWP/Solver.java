package Solvers.TWP;

import SimulatorCore.*;

import static Solvers.TWP.SpatioTemporalGraph.AStar;

import java.util.*;

public class Solver implements Service {
    private static final Random random = new Random(0);
    private SpatioTemporalGraph SPG;
    public Solver() { }

    @Override
    public void initialize(PickerGroup pg, RackGroup rg) {
        this.SPG = new SpatioTemporalGraph(Ground.INSTANCE.getHeight(), Ground.INSTANCE.getWidth(), rg);
    }

    @Override
    public HashMap<Integer, Integer> assign(int currentTime, ArrayList<Robot> availRobot, HashMap<Integer, Task> availTasks) {
        this.SPG.update(currentTime);
        HashMap<Integer, Integer> assignment = new HashMap<>();
        List<Integer> taskIds = new ArrayList<>(availTasks.keySet());
        Collections.shuffle(taskIds, random);
        for (int i = 0; i < Math.min(availRobot.size(), taskIds.size()); ++i) {
            assignment.put(taskIds.get(i), availRobot.get(i).getRobotId());
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
