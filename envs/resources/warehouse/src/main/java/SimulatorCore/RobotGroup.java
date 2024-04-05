package SimulatorCore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

public enum RobotGroup {
    INSTANCE;
    private static final HashMap<Integer, Robot> idToRobot = Ground.INSTANCE.getIdToRobot();
    RobotGroup() {}

    public boolean update(int t) {
        return idToRobot.values().stream().map(r -> r.update(t)).reduce(Boolean::logicalOr).orElse(Boolean.FALSE);
    }

    public ArrayList<Robot> getAvailRobot() {
        return (ArrayList<Robot>) idToRobot.values().stream().filter(Robot::isIdling).collect(Collectors.toList());
    }

    public void dispatchTask(int t, HashMap<Integer, Integer> assignment, HashMap<Integer, Path> robotIdToPath,
                             HashMap<Integer, Task> tasks) {
        assignment.keySet().forEach(taskId -> {
            Robot robot = getRobotById(assignment.get(taskId));
            robot.dispatch(t, tasks.get(taskId), robotIdToPath.get(robot.getRobotId()));
        });
    }

    public ArrayList<Robot> getReadyReturnRobot() {
        return (ArrayList<Robot>) idToRobot.values().stream().filter(Robot::isReadyToReturn).collect(Collectors.toList());
    }

    public void dispatchReturn(HashMap<Integer, Path> robotIdToPath) {
        robotIdToPath.keySet().forEach(robotId -> idToRobot.get(robotId).setPath(robotIdToPath.get(robotId)));
    }

    public Robot getRobotById(Integer robotId) {
        return idToRobot.get(robotId);
    }

    public void outputStatistics(String prefix) {
        idToRobot.values().forEach(r -> System.out.println(r.statistics(prefix)));
    }

    public double getWorkingRate(int currentTime) {
        return idToRobot.values().stream().mapToDouble(p -> p.getSingleWorkingRate(currentTime)).average().orElse(0.);
    }

    public double getQueuingTime() {
        return idToRobot.values().stream().mapToDouble(Robot::getSingleQueuingTime).average().orElse(0.);
    }

    public double[] getWorkingTimeBreakDown() {
        // idling, emptymoving, carry moving, queuing 
        double[] times = new double[4];
        times[0] = idToRobot.values().stream().mapToDouble(Robot::getIdleTime).average().orElse(0.);
        times[1] = idToRobot.values().stream().mapToDouble(Robot::getEmptyMovingDistance).average().orElse(0.);
        times[2] = idToRobot.values().stream().mapToDouble(Robot::getCarriedMovingDistance).average().orElse(0.);
        times[3] = getQueuingTime();
        return times;
    }
}
