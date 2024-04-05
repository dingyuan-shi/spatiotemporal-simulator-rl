package SimulatorCore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import org.apache.commons.lang3.tuple.Pair;
public interface Service {

    default HashMap<Integer, Integer> assign(int currentTime, ArrayList<Robot> availRobot, HashMap<Integer, Task> availTasks) {
        // random
        HashMap<Integer, Integer> assignment = new HashMap<>();
        ArrayList<Task> tasks = new ArrayList<>();
        for (Integer taskId: availTasks.keySet()) {
            tasks.add(availTasks.get(taskId));
        }
        for (int i = 0; i < Math.min(availRobot.size(), tasks.size()); ++i) {
            assignment.put(tasks.get(i).getRackId(), availRobot.get(i).getRobotId());
        }
        return assignment;
    }

    default void initialize(PickerGroup pg, RackGroup rg) { }

    Object getIndex();

    default HashMap<Integer, Path> planning(int currentTime, HashMap<Integer, Integer> assignment, HashMap<Integer, Task> availTasks) {
        HashMap<Integer, Path> robotIdToPath = new HashMap<>();
        for (Integer taskId: assignment.keySet()) {
            int robotId = assignment.get(taskId);
            Robot r = Convertor.getRobotById(robotId);
            Rack rack = Convertor.taskToRack(availTasks.get(taskId));
            Picker p = Convertor.rackToPicker(rack);
            int robotToRackDistance = rack.getLocation().manhattanDistance(r.getLocation());
            int rackToPickerDistance = p.getLocation().manhattanDistance(rack.getLocation());
            robotIdToPath.put(robotId, new Path(
                    new ArrayList<>(Arrays.asList(
                            Pair.of(robotToRackDistance, rack.getLocation()),
                            Pair.of(robotToRackDistance + rackToPickerDistance, p.getLocation())
                    )),
                    currentTime
            ));
        }
        return robotIdToPath;
    };

    default HashMap<Integer, Path> planning(int currentTime, ArrayList<Robot> readyReturnRobot) {
        HashMap<Integer, Path> robotIdToPath = new HashMap<>();
        for (Robot r: readyReturnRobot) {
            Location target = Convertor.robotToRack(r).getLocation();
            robotIdToPath.put(r.getRobotId(),
                    new Path(
                            new ArrayList<>(
                                    Arrays.asList(
                                            Pair.of(r.getLocation().manhattanDistance(target), target)
                                    )),
                            currentTime));
        }
        return robotIdToPath;
    };
}
