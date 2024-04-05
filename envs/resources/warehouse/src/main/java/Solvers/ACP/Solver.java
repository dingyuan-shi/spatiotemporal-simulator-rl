package Solvers.ACP;
import SimulatorCore.*;
import org.apache.commons.lang3.tuple.Pair;
import java.util.*;
import java.util.stream.Collectors;

public class Solver implements Service {
    private SpatioTemporalGraph SPG;
    // private final double[][] q;
    private final HashMap<String, double[]> q;
    private static final double alpha = 0.1;
    private static final double gamma = 0.9;
    private static final double delta = 0.2;
    private int[] pickerTime;
    private int[] rackTime;
    // paramters for rack5000 dataset
    // private static final int MAX_PICKER_LEN = 60000;
    // private static final int PICKER_SEG_LEN = 100;

    private static final int PICKER_SEG_LEN = 1000;
    // private static final int MAX_RACK_LEN = 1000;
    // private static final int RACK_SEG_LEN = 10;
    private static final int RACK_SEG_LEN = 100;
    private static final int THRESH = 50;
    private int lastClearTime;
    private int lastClearRack;
    private int lastClearPicker;


    // parameters for rack7000 dataset
//    private static final int MAX_PICKER_LEN = 420000;
//    private static final int PICKER_SEG_LEN = 1000;
//    private static final int MAX_RACK_LEN = 40000;
//    private static final int RACK_SEG_LEN = 1000;
//    private static final int THRESH = 50;

    // parameters for rack65000 dataset
//    private static final int MAX_PICKER_LEN = 1800000;
//    private static final int PICKER_SEG_LEN = 10000;
//    private static final int MAX_RACK_LEN = 62000;
//    private static final int RACK_SEG_LEN = 1000;
//    private static final int THRESH = 50;

    private HashMap<Integer, Set<Integer>> pickerIdToRackIds;
    private final Random random = new Random(0);
    private HashMap<Integer, ArrayList<Integer>> gridToRacks;
    // private final int STATE_NUM;

    public Solver () {
        // STATE_NUM = MAX_PICKER_LEN / PICKER_SEG_LEN * MAX_RACK_LEN / RACK_SEG_LEN;
        // q = new double[STATE_NUM][2];
        q = new HashMap<>();
        lastClearTime = 0;
        lastClearPicker = 0;
        lastClearRack = 0;
    }

    @Override
    public void initialize(PickerGroup pg, RackGroup rg) {
        SPG = new SpatioTemporalGraph(Ground.INSTANCE.getHeight(), Ground.INSTANCE.getWidth(), rg);
        int pickerNum =  pg.getPickerNum();
        int rackNum = rg.getRackNum();
        pickerTime = new int[pickerNum];
        rackTime = new int[rackNum];
        pickerIdToRackIds = new HashMap<>();
        gridToRacks = new HashMap<>();
        int height = Ground.INSTANCE.getHeight();
        int width = Ground.INSTANCE.getWidth();
        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                gridToRacks.put(i * width + j, new ArrayList<>());
                Location currentLoc = new Location(i, j);
                for (int k = 0; k < rackNum; ++k) {
                    if (currentLoc.manhattanDistance(Convertor.getRackById(k).getLocation()) < THRESH) {
                        gridToRacks.get(i * width + j).add(k);
                    }
                }
            }
        }
        for (int i = 0; i < pickerNum; ++i) {
            pickerIdToRackIds.put(i, new HashSet<>());
        }
        for (int i = 0; i < rackNum; ++i) {
            pickerIdToRackIds.get(Convertor.rackToPickerId(i)).add(i);
        }
    }

    private String getStateNum(int pickerLen, int rackLen) {
         // int i = Math.min(pickerLen / PICKER_SEG_LEN, MAX_PICKER_LEN / PICKER_SEG_LEN - 1);
         // int j = Math.min(rackLen / RACK_SEG_LEN, MAX_RACK_LEN / RACK_SEG_LEN - 1);
         // return i * MAX_RACK_LEN / RACK_SEG_LEN + j;
        return pickerLen / PICKER_SEG_LEN + "#" + rackLen / RACK_SEG_LEN;
    }

    private Pair<Integer, Integer> findClosestRobot(ArrayList<Robot> availRobot, HashSet<Integer> assigned, Location taskLocation) {
        int recMinDis = -1;
        int recRobotId = -1;
        for (Robot r: availRobot) {
            if (assigned.contains(r.getRobotId())) continue;
            int dis = taskLocation.manhattanDistance(r.getLocation());
            if (recMinDis == -1 || recMinDis > dis) {
                recMinDis = dis;
                recRobotId = r.getRobotId();
            }
        }
        return Pair.of(recRobotId, recMinDis);
    }

    private void clear(int currentTime) {
        if (currentTime - lastClearTime < 10000) return;
        for (int i = lastClearPicker; i < currentTime / 10; ++i) {
            for (int j = lastClearRack; j < currentTime / 100; ++j) {
                q.remove(i + "#" + j);
            }
        }
        lastClearTime = currentTime;
        lastClearPicker = currentTime / 5;
        lastClearRack = currentTime / 50;
    }
    @Override
    public HashMap<Integer, Integer> assign(int currentTime, ArrayList<Robot> availRobot, HashMap<Integer, Task> availTasks) {
        clear(currentTime);
        SPG.update(currentTime);
        HashSet<Integer> assignedRobotIds = new HashSet<>();
        HashMap<Integer, Integer> assignment = new HashMap<>();
        if (random.nextDouble() < delta) {
            ArrayList<Integer> availPickers = (ArrayList<Integer>) availTasks.keySet().stream()
                    .map(Convertor::rackToPickerId)
                    .distinct().collect(Collectors.toList());
            availPickers.sort(Comparator.comparingInt(o -> Convertor.getPickerById(o).getFinishTime()));
            for (int pickerId: availPickers) {
                Picker picker = Convertor.getPickerById(pickerId);
                for (Integer taskId : pickerIdToRackIds.get(pickerId)) {
                    if (!availTasks.containsKey(taskId)) continue;
                    Task task = availTasks.get(taskId);
                    Rack rack = Convertor.taskToRack(task);
                    Location taskLocation = rack.getLocation();
                    Pair<Integer, Integer> recRobotIdMinDis = findClosestRobot(availRobot, assignedRobotIds, taskLocation);
                    int recRobot = recRobotIdMinDis.getKey();
                    int recMinDis = recRobotIdMinDis.getValue();
                    // int state = getStateNum(pickerTime[pickerId], rackTime[taskId]);
                    String state = getStateNum(pickerTime[pickerId], rackTime[taskId]);
                    if (recRobot == -1) {
                        // q[state][0] += alpha * (-1. + gamma * Math.max(q[state][1], q[state][0]) - q[state][0]);
                        if (!q.containsKey(state)) q.put(state, new double[2]);
                        q.get(state)[0] += alpha * (-1. + gamma * Math.max(q.get(state)[1], q.get(state)[0]) - q.get(state)[0]);
                        continue;
                    }
                    rackTime[taskId] += task.getPickingTime();
                    pickerTime[pickerId] += task.getPickingTime();
                    // int nextState = getStateNum(pickerTime[pickerId], rackTime[taskId]);
                    String nextState = getStateNum(pickerTime[pickerId], rackTime[taskId]);
                    int pickUpAndDeliver = recMinDis + picker.getLocation().manhattanDistance(taskLocation);
                    double r = -Math.max(picker.getFinishTime(), pickUpAndDeliver) - task.getPickingTime();
                    // q[state][1] += alpha * (r + Math.pow(gamma, -r) * Math.max(q[nextState][1], q[nextState][0]) - q[state][1]);
                    if (!q.containsKey(state)) q.put(state, new double[2]);
                    if (!q.containsKey(nextState)) q.put(nextState, new double[2]);
                    q.get(state)[1] += alpha * (r + Math.pow(gamma, -r) * Math.max(q.get(nextState)[1], q.get(nextState)[0]) - q.get(state)[1]);
                    assignedRobotIds.add(recRobot);
                    assignment.put(taskId, recRobot);
                }
            }
            return assignment;
        }
        HashSet<Integer> assignedTasks = new HashSet<>();
        for (Robot robot: availRobot) {
            Location robotLocation = robot.getLocation();
            ArrayList<Integer> racks = gridToRacks.get(robotLocation.getR() * Ground.INSTANCE.getWidth() + robotLocation.getC());
            double maxQ = -100000000;
            int recMaxQTask = -1;
            int minDis = -1;
            int recMinDisTask = -1;
            if (random.nextDouble() < 0.05) {  // epsilon-greedy
                continue;
            }
            for (Integer rackId: racks) {
                if (assignedTasks.contains(rackId) || !availTasks.containsKey(rackId)) continue;
                Location rackLocation = Convertor.getRackById(rackId).getLocation();
                Picker picker = Convertor.rackToPicker(rackId);
                // int state = getStateNum(pickerTime[picker.getPickerId()], rackTime[rackId]);
                String state = getStateNum(pickerTime[picker.getPickerId()], rackTime[rackId]);
                // epsilon-greedy
                // int action = q[state][0] < q[state][1]?1:0;
                if (!q.containsKey(state)) q.put(state, new double[2]);
                int action = q.get(state)[0] < q.get(state)[1]?1:0;
                if (random.nextDouble() < 0.05) {  // epsilon-greedy
                    action = random.nextInt(2);
                }
                if (action == 0) {
                    // q[state][0] += alpha * (-1. + gamma * Math.max(q[state][1], q[state][0]) - q[state][0]);
                    q.get(state)[0] += alpha * (-1. + gamma * Math.max(q.get(state)[1], q.get(state)[0]) - q.get(state)[0]);
                    continue;
                }
//                if (maxQ < -q[state][0]) {
//                    maxQ = -q[state][0];
//                    recMaxQTask = rackId;
//                }
                if (maxQ < -q.get(state)[0]) {
                    maxQ = -q.get(state)[0];
                    recMaxQTask = rackId;
                }
                int dis = robotLocation.manhattanDistance(rackLocation);
                if (minDis == -1 || minDis > dis) {
                    minDis = dis;
                    recMinDisTask = rackId;
                }
            }
            int chooseTaskId = recMaxQTask;
            if (chooseTaskId == -1) continue;
            Task chooseTask = availTasks.get(chooseTaskId);
            Picker picker = Convertor.taskToPicker(chooseTask);
            Rack rack = Convertor.getRackById(chooseTaskId);
            // int state = getStateNum(pickerTime[picker.getPickerId()], rackTime[chooseTaskId]);
            String state = getStateNum(pickerTime[picker.getPickerId()], rackTime[chooseTaskId]);
            assignedTasks.add(chooseTaskId);
            assignment.put(chooseTaskId, robot.getRobotId());
            pickerTime[picker.getPickerId()] += chooseTask.getPickingTime();
            rackTime[chooseTaskId] += chooseTask.getPickingTime();
            // int nextState = getStateNum(pickerTime[picker.getPickerId()], rackTime[chooseTaskId]);
            String nextState = getStateNum(pickerTime[picker.getPickerId()], rackTime[chooseTaskId]);
            int pickUpAndDeliver = robot.getLocation().manhattanDistance(rack.getLocation())
                    + picker.getLocation().manhattanDistance(rack.getLocation());
            double r = -Math.max(picker.getFinishTime(), pickUpAndDeliver) - chooseTask.getPickingTime();
            if (!q.containsKey(state)) q.put(state, new double[2]);
            if (!q.containsKey(nextState)) q.put(nextState, new double[2]);
            // q[state][1] += alpha * (r + Math.pow(gamma, -r) * Math.max(q[nextState][1], q[nextState][0]) - q[state][1]);
            q.get(state)[1] += alpha * (r + Math.pow(gamma, -r) * Math.max(q.get(nextState)[1], q.get(nextState)[0]) - q.get(state)[1]);
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
                   new int[]{rack.getLocation().getR(), rack.getLocation().getC()});
           Path rackToPicker = AStar(currentTime + robotToRack.getFinishTime() + 1,
                   new int[]{rack.getLocation().getR(), rack.getLocation().getC()},
                   new int[]{p.getLocation().getR(), p.getLocation().getC()});
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
                   new int[]{target.getR(), target.getC()});
           robotIdToPath.put(r.getRobotId(), p);
           SPG.insertSinglePath(p);
       }
       return robotIdToPath;
   }

    private Path AStar(int currentTime, int[] src, int[] dst) {
        int[] dr = {0, 0, 0, 1, -1};
        int[] dc = {0, 1, -1, 0, 0};
        int srcR = src[0];
        int srcC = src[1];
        int dstR = dst[0];
        int dstC = dst[1];
        Node lastNode = null;
        PriorityQueue<Node> q = new PriorityQueue<>(
                (o1, o2) -> o1.getH() - o2.getH() == 0?Math.abs(o1.getC() - dstC) + Math.abs(o1.getR() - dstR) -
                        (Math.abs(o2.getC() - dstC) + Math.abs(o2.getR() - dstR)):o1.getH() - o2.getH()
        );
        q.offer(new Node(srcR, srcC, 0, 0, 0, null));
        ArrayList<Pair<Integer, Location>> traj = null;
        while (!q.isEmpty() && lastNode == null) {
            Node current = q.poll();
            SPG.setVis(current.getT() + currentTime, current.getR(), current.getC());
            traj = Memory.get(current.getR(), current.getC(), dstR, dstC);
            if (traj != null) {
                traj = SPG.getTrajectoryAfterAdjust(current.getT() + currentTime, traj);
                lastNode = current;
                break;
            }
            for (int i = 0; i < 5; ++i) {
                int nextT = current.getT() + 1;
                int nextR = current.getR() + dr[i];
                int nextC = current.getC() + dc[i];
                if (nextR == dstR && nextC == dstC) {
                    lastNode = current;
                    break;
                }
                if ((nextR != srcR || nextC != srcC) && (SPG.getSpatioTemporalPoint(nextT + currentTime, nextR, nextC) == -1 || SPG.isVis(nextT + currentTime, nextR, nextC))) continue;
                int nextG = current.getG() + Math.abs(nextR - current.getR()) + Math.abs(nextC - current.getC());
                int nextH = Math.abs(nextR - dstR) + Math.abs(nextC - dstC) + nextT;
                Node recN = null;
                for (Node n: q) {
                    if (n.getR() == nextR && n.getC() == nextC && n.getT() == nextT) {
                        recN = n;
                        break;
                    }
                }
                if (recN != null) {
                    if (recN.getG() > nextG) {
                        recN.setG(nextG);
                        recN.setPre(current);
                    }
                } else {
                    q.offer(new Node(nextR, nextC, nextT, nextG, nextH, current));
                }
            }
        }
        ArrayList<Pair<Integer, Location>> trajectory = new ArrayList<>();
        trajectory.add(Pair.of(lastNode.getT() + 1, new Location(dstR, dstC)));
        while (lastNode != null) {
            trajectory.add(Pair.of(lastNode.getT(), new Location(lastNode.getR(), lastNode.getC())));
            lastNode = lastNode.getPre();
        }
        Collections.reverse(trajectory);
        if (traj != null) trajectory.addAll(traj);
        else Memory.put(trajectory);
        return new Path(trajectory, currentTime);
    }

    @Override
    public Object getIndex() {
        return this.SPG;
    }
}
