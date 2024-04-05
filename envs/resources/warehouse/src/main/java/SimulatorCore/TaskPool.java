package SimulatorCore;

import java.util.HashMap;
import java.util.Map;

import com.opencsv.exceptions.CsvValidationException;


public enum TaskPool {
    INSTANCE;
    private static final TaskLoader taskLoader = TaskLoader.INSTANCE;
    private static final HashMap<Integer, Integer> rackIdToTaskTime = new HashMap<>();
    private static final HashMap<Integer, Integer> rackIdToTaskNumber = new HashMap<>();
    private final Task taskPrototype;
    TaskPool() {
        taskPrototype = new Task(0, 0, 0, 0);
    }

    public void setIgnorePickingTime(boolean ignorePickingTime) {
        taskLoader.setIgnorePickingTime(ignorePickingTime);
    }

    public HashMap<Integer, Task> getTasks(int currentTime) throws CsvValidationException, NumberFormatException {
        HashMap<Integer, Integer> incrementalTime = taskLoader.getTask(currentTime);
        HashMap<Integer, Integer> incrementalNum = taskLoader.getTaskNum();
        for (Integer rackId: incrementalTime.keySet()) {
            rackIdToTaskTime.put(rackId, rackIdToTaskTime.getOrDefault(rackId, 0) + incrementalTime.get(rackId));
            rackIdToTaskNumber.put(rackId, rackIdToTaskNumber.getOrDefault(rackId, 0) + incrementalNum.get(rackId));
        }
        HashMap<Integer, Task> res = new HashMap<>();
        for (Map.Entry<Integer, Integer> t: rackIdToTaskTime.entrySet()) {
            if (t.getValue() > 0 && RackGroup.INSTANCE.isRackAvail(t.getKey())) {
                try {
                    Task tmp = (Task) taskPrototype.clone();
                    tmp.setAll(t.getKey(), t.getKey(), t.getValue(), currentTime);
                    res.put(t.getKey(), tmp);
                } catch (CloneNotSupportedException e) {}
            }
        }
        return res;
    }

    public int removeAssigned(HashMap<Integer, Integer> assignment) {
        assignment.keySet().forEach(rackId -> {
            rackIdToTaskTime.put(rackId, 0);
            RackGroup.INSTANCE.updateNotAvail(rackId);
        });
        int assignedTaskNum = 0;
        for (Integer rackId: assignment.keySet()) {
           assignedTaskNum += rackIdToTaskNumber.get(rackId);
           rackIdToTaskNumber.put(rackId, 0);
        }
        return assignedTaskNum;
    }


    public boolean hasNext() {
        return rackIdToTaskTime.values().stream().max(Integer::compare).orElse(0) > 0|| TaskLoader.INSTANCE.hasNext();
    }

    public int[] getTaskNumberTimeOnRack(int rackId) {
        int[] res = new int[2];
        res[0] = rackIdToTaskNumber.getOrDefault(rackId, 0);
        res[1] = rackIdToTaskTime.getOrDefault(rackId,  0);
        return res;
    }
}
