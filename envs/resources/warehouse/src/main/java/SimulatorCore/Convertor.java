package SimulatorCore;

public class Convertor {

    public static Picker rackToPicker(int rackId) {
        return rackToPicker(RackGroup.INSTANCE.getRackById(rackId));
    }

    public static int rackToPickerId(int rackId) {
        return rackToPicker(rackId).getPickerId();
    }

    public static int rackToPickerId(Rack rack) {
        return rackToPicker(rack).getPickerId();
    }

    public static Picker rackToPicker(Rack rack) {
        return PickerGroup.INSTANCE.getPickerById(rack.getPickerId());
    }


    public static Rack robotToRack(Robot r) {
        return RackGroup.INSTANCE.getRackById(r.getAssigned().getRackId());
    }

    public static Rack robotToRack(int robotId) {
        return robotToRack(RobotGroup.INSTANCE.getRobotById(robotId));
    }

    public static Picker robotToPicker(int robotId) {
        return robotToPicker(RobotGroup.INSTANCE.getRobotById(robotId));
    }

    public static Picker robotToPicker(Robot robot) {
        return PickerGroup.INSTANCE.getPickerById(RackGroup.INSTANCE.getRackById(robot.getAssigned().getRackId()).getPickerId());
    }

    public static Robot getRobotById(int robotId) {
        return RobotGroup.INSTANCE.getRobotById(robotId);
    }

    public static Picker getPickerById(int pickerId) {
        return PickerGroup.INSTANCE.getPickerById(pickerId);
    }

    public static Rack getRackById(int rackId) {
        return RackGroup.INSTANCE.getRackById(rackId);
    }

    public static Rack taskToRack(Task t) {
        return RackGroup.INSTANCE.getRackById(t.getRackId());
    }

    public static Picker taskToPicker(Task t) {
        return rackToPicker(taskToRack(t));
    }
}
