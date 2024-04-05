package SimulatorCore;

import UserConfig.Configurations;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import org.apache.commons.lang3.tuple.Pair;

public enum Ground {
    INSTANCE;
    private int width;
    private int height;
    private final HashMap<Integer, Rack> idToRack;
    private final HashMap<Integer, Picker> idToPicker;
    private final HashMap<Integer, Robot> idToRobot;

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public HashMap<Integer, Rack> getIdToRack() {
        return idToRack;
    }

    public HashMap<Integer, Picker> getIdToPicker() {
        return idToPicker;
    }

    public HashMap<Integer, Robot> getIdToRobot() {
        return idToRobot;
    }

    Ground() {
        HashMap<Integer, Integer> rackToPickers = new HashMap<>();
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(
                            String.valueOf(Paths.get(Configurations.RACK_TO_PICKERS_PATH))), StandardCharsets.UTF_8));
            String line;
            while((line = reader.readLine()) != null){
                String[] item = line.split(",");
                rackToPickers.put(Integer.parseInt(item[0]), Integer.parseInt(item[1]));
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        idToRack = new HashMap<>();
        idToPicker = new HashMap<>();
        idToRobot = new HashMap<>();
        ArrayList<Pair<Integer, Location>> rackLocations = new ArrayList<>();
        try {
            BufferedReader layout = new BufferedReader(new InputStreamReader(
                    new FileInputStream(UserConfig.Configurations.LAYOUT_FILE_PATH)));
            String strTmp = layout.readLine();
            height = 0;
            for (int r = 0; strTmp != null; strTmp = layout.readLine(), ++r, ++height) {
                if (height == 0) width = strTmp.length();
                else if (width != strTmp.length()) {
                    System.out.println("Width not consistence at line " + r);
                    System.exit(1);
                }
                for (int c = 0; c < strTmp.length(); c++) {
                    char cur = strTmp.charAt(c);
                    if (cur ==  'P') {
                        Picker picker = new Picker(new Location(r, c));
                        idToPicker.put(picker.getPickerId(), picker);
                    } else if (cur == 'A') {
                        Robot robot = new Robot(new Location(r, c));
                        idToRobot.put(robot.getRobotId(), robot);
                    } else if (cur == 'R') {
                        rackLocations.add(Pair.of(rackLocations.size(), new Location(r, c)));
                    } else if (cur != '-') {
                        System.out.printf("Warning: unexpected character %c at %d, %d \n", cur, r, c);
                    }
                }
            }
            layout.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        HashMap<Integer, Integer> pairRack = findDenseRack(rackLocations);
        for (Pair<Integer, Location> rackLocation: rackLocations) {
            Rack rack = new Rack(rackLocation.getValue(), rackToPickers.get(idToRack.size()),
                    pairRack.getOrDefault(rackLocation.getKey(), -1));
            idToRack.put(rack.getRackId(), rack);
        }
        System.out.printf("%02dx%02d ground, with %d racks, %d robots and %d pickers\n",
                width, height, idToRack.size(), idToRobot.size(), idToPicker.size());
//        System.out.println("dense: ");
//        int cnt = 0;
//        for (Integer id: pairRack.keySet()) {
//            System.out.printf("%d-p-%d ", id, pairRack.get(id));
//            if (++cnt == 7) {
//                System.out.println();
//                cnt = 0;
//            }
//        }
//        if (cnt != 0) System.out.println();
    }

    private HashMap<Integer, Integer> findDenseRack(ArrayList<Pair<Integer, Location>> rackLocations) {
        HashSet<Integer> rowDense = new HashSet<>();
        HashMap<Integer, Integer> pairedRackSuper = new HashMap<>();
        for (int i = 1; i < rackLocations.size() - 1; ++i) {
            if (rackLocations.get(i).getValue().getR() == rackLocations.get(i - 1).getValue().getR()
                    && rackLocations.get(i).getValue().getR() == rackLocations.get(i + 1).getValue().getR()
                    && rackLocations.get(i).getValue().getC() == rackLocations.get(i - 1).getValue().getC() + 1
                    && rackLocations.get(i).getValue().getC() == rackLocations.get(i + 1).getValue().getC() - 1) {
                int rackNo = rackLocations.get(i).getKey();
                rowDense.add(rackNo);
                // 再往左一个就换行了 或者间隔很远 说明只需要跨越前一个就能到过道上
                if (i < 2 || rackLocations.get(i).getValue().getR() != rackLocations.get(i - 2).getValue().getR()
                        || rackLocations.get(i).getValue().getC() - rackLocations.get(i - 2).getValue().getC() > 2) {
                    pairedRackSuper.put(rackNo, rackLocations.get(i - 1).getKey());
                } else if (i >= rackLocations.size() - 2
                        || rackLocations.get(i + 2).getValue().getR() != rackLocations.get(i).getValue().getR()
                        || rackLocations.get(i + 2).getValue().getC() - rackLocations.get(i).getValue().getR() > 2) {
                    pairedRackSuper.put(rackNo, rackLocations.get(i + 1).getKey());
                }
            }
        }
        rackLocations.sort((o1, o2) -> o1.getValue().getC() != o2.getValue().getC()?
                o1.getValue().getC() - o2.getValue().getC():o1.getValue().getR() - o2.getValue().getR());
        HashMap<Integer, Integer> pairRack = new HashMap<>();
        for (int i = 1; i < rackLocations.size() - 1; ++i) {
            if (rackLocations.get(i).getValue().getC() == rackLocations.get(i - 1).getValue().getC()
                    && rackLocations.get(i).getValue().getC() == rackLocations.get(i + 1).getValue().getC()
                    && rackLocations.get(i - 1).getValue().getR() + 1 == rackLocations.get(i).getValue().getR()
                    && rackLocations.get(i + 1).getValue().getR() - 1 == rackLocations.get(i).getValue().getR()
                    && rowDense.contains(rackLocations.get(i).getKey())) {
                pairRack.put(rackLocations.get(i).getKey(), pairedRackSuper.get(rackLocations.get(i).getKey()));
            }
        }
        return pairRack;
    }
}
