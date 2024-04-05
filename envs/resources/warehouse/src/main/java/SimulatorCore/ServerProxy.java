package SimulatorCore;

import java.util.ArrayList;
import java.util.HashMap;

public class ServerProxy {
    private final Service service;

    public Object getIndex() {
        return service.getIndex();
    }

    public ServerProxy(Service service) {
        this.service = service;
        this.service.initialize(PickerGroup.INSTANCE, RackGroup.INSTANCE);
    }

    public static HashMap<Integer, Path> planningBack (HashMap<Integer, Path> robotIdToPath) {
        // 为路径添加关键点
        for (Integer robotId: robotIdToPath.keySet()) {
            robotIdToPath.get(robotId).setRackLocation(
                    Convertor.robotToRack(robotId).getLocation());
        }
        return robotIdToPath;
    }

    public static HashMap<Integer, Path> planningTo(HashMap<Integer, Integer> assignment, HashMap<Integer, Task> availTasks, HashMap<Integer, Path> robotIdToPath) {
        for (Integer taskId: assignment.keySet()) {
            int robotId = assignment.get(taskId);
            Rack rack = Convertor.taskToRack(availTasks.get(taskId));
            robotIdToPath.get(robotId).setRackLocation(rack.getLocation());
            robotIdToPath.get(robotId).setPickerLocation(Convertor.rackToPicker(rack).getLocation());
        }
        return robotIdToPath;
    }

    public HashMap<Integer, Path> planning(int currentTime, ArrayList<Robot> readyReturnRobot) {
        HashMap<Integer, Path> robotIdToPath = service.planning(currentTime, readyReturnRobot);  // as a proxy of service
        return planningBack(robotIdToPath);
    }

    public HashMap<Integer, Integer> assign(int currentTime, ArrayList<Robot> availRobot, HashMap<Integer, Task> newTasks) {
        return service.assign(currentTime, availRobot, newTasks);
    }

    public HashMap<Integer, Path> planning(int currentTime, HashMap<Integer, Integer> assignment,
                                           HashMap<Integer, Task> availTasks) {
        HashMap<Integer, Path> robotIdToPath = service.planning(currentTime, assignment, availTasks); // as a proxy
        return planningTo(assignment, availTasks, robotIdToPath);
    }
}
