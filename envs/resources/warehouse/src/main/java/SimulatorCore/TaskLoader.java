package SimulatorCore;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import com.opencsv.CSVReader;


public enum TaskLoader {
    INSTANCE;
    private static final int BUFFER_SIZE = Settings.TASK_BUFFER_SIZE;
    private Task taskPrototype;
    private Task[] buffer;
    private int pointer = 0;
    private int size = 0;
    private boolean ignorePickingTime = false;
    private CSVReader reader;
    private static boolean hasTask;
    private static final HashMap<Integer, Integer> rackToTaskTime = new HashMap<>();
    private static final HashMap<Integer, Integer> rackToTaskNum = new HashMap<>();

    TaskLoader() {
        try {
            taskPrototype = new Task(0, 0, 0, 0);
            this.reader = new CSVReader(new FileReader(UserConfig.Configurations.TASK_FILE_PATH));
            buffer = new Task[BUFFER_SIZE + 5];
            loadTasks();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setIgnorePickingTime(boolean ignorePickingTime) {
        this.ignorePickingTime = ignorePickingTime;
    }

    private void loadTasks() {
        size = 0;
        pointer = 0;
        String[] line;
        // caution: judge the size first
        try {
            while (size < BUFFER_SIZE && (line = reader.readNext()) != null) {
                try {
                    Task tmp = (Task) taskPrototype.clone();
                    tmp.setAll(Integer.parseInt(line[0]), Integer.parseInt(line[1]), ignorePickingTime?1:Integer.parseInt(line[2]), Integer.parseInt(line[3]));
                    buffer[size++] = tmp;
                } catch (CloneNotSupportedException e) {}
            }
        } catch (Exception e) {
            e.printStackTrace();
        } 
        hasTask = size > 0;
    }

    public HashMap<Integer, Integer> getTask(int currentTime) {
        rackToTaskTime.clear();
        rackToTaskNum.clear();
        while (pointer < size && buffer[pointer].getTimestamp() <= currentTime) {
            rackToTaskTime.put(buffer[pointer].getRackId(),
                    rackToTaskTime.getOrDefault(buffer[pointer].getRackId(), 0) + buffer[pointer].getPickingTime());
            rackToTaskNum.put(buffer[pointer].getRackId(),
                    rackToTaskNum.getOrDefault(buffer[pointer].getRackId(), 0) + 1);
            if ((++pointer) == size) {
                try {
                    loadTasks();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return rackToTaskTime;
    }

    public HashMap<Integer, Integer> getTaskNum() {
        return rackToTaskNum;
    }

    public boolean hasNext() {
        return hasTask;
    }
}
