package SimulatorCore;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import UserConfig.Configurations;

public class Snapshot {
    private int width;
    private int height;
    private char[] view;
    private char[] staticView;
    private int[][] heat;
    private BufferedWriter writer;
    private BufferedWriter statsWriter;
    private String[] buffer;
    private int p;
    
    public Snapshot () {
        this.width = Ground.INSTANCE.getWidth();
        this.height = Ground.INSTANCE.getHeight();
        File meta = new File(Configurations.EVAL_PATH, "meta.json");
        try {
            meta.createNewFile();
            FileWriter metaWriter = new FileWriter(meta, false);
            metaWriter.append("{\"width\": " + this.width + ", \"height\": "
                 + this.height + ", \"stat_freq\": " + Configurations.LOG_SEG + "}");
            metaWriter.flush();
            metaWriter.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        File frameFile = new File(Configurations.EVAL_PATH, "frames.txt");
        
        this.view = new char[width * height];
        this.staticView = new char[width * height];
        this.buffer = new String[128];
        this.p = 0;

        this.heat = new int[height][width];
        try {
            frameFile.createNewFile();
            this.writer = new BufferedWriter(
                            new OutputStreamWriter(new FileOutputStream(frameFile, false)));
            File statsFile = new File(Configurations.EVAL_PATH, "stats.txt");
            this.statsWriter = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(statsFile, false))); 
        } catch (IOException e) {
            e.printStackTrace();
        }
        Arrays.fill(view, '.');
        Ground.INSTANCE.getIdToPicker().values().forEach(picker -> {
            Location loc = picker.getLocation();
            view[loc.getR() * this.width + loc.getC()] = 'P';
        });
        Ground.INSTANCE.getIdToRack().values().forEach(rack -> {
            Location loc = rack.getLocation();
            view[loc.getR() * this.width + loc.getC()] = 'R';
        });
        this.staticView = Arrays.copyOf(this.view, width * height);
        Ground.INSTANCE.getIdToRobot().values().forEach(robot -> {
            Location loc = robot.getLocation();
            view[loc.getR() * this.width + loc.getC()] = 'A';
        });
        this.buffer[this.p++] = String.valueOf(this.view);
    }
    
    public void dump(boolean done, int currentTime) {
        // dump the statistics
        if (done || currentTime % Configurations.LOG_SEG == 0) {
            double ppr = PickerGroup.INSTANCE.getWorkingRate();
            double rwr = RobotGroup.INSTANCE.getWorkingRate(currentTime + 1);
            double[] timebreakdown = RobotGroup.INSTANCE.getWorkingTimeBreakDown();
            int[][] top10 = RackGroup.INSTANCE.top10Racks();
            try {
                this.statsWriter.append(String.format("%.3f %.3f\n", ppr, rwr));
                for (int i = 0; i < timebreakdown.length; ++i) {
                    this.statsWriter.append(String.format("%.3f ", timebreakdown[i]));
                }
                this.statsWriter.append('\n');
                for (int i = 0; i < top10.length; ++i) {
                    for (int j = 0; j < top10[i].length; ++j) {
                        this.statsWriter.append(String.format("%d ", top10[i][j]));
                    }
                    this.statsWriter.append('\n');
                }
                for (int i = 0; i < this.heat.length; ++i) {
                    for (int j = 0; j < this.heat[i].length; ++j) {
                        this.statsWriter.append(String.format("%d ", this.heat[i][j]));
                    }
                    this.statsWriter.append('\n');
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // dump the view
        this.view = Arrays.copyOf(this.staticView, width * height);
        Ground.INSTANCE.getIdToRobot().values().forEach(robot -> {
            Task task = robot.getAssigned();
            Location loc = robot.currentLocation(currentTime);
            this.heat[loc.getR()][loc.getC()]++;
            if (!robot.carried()) {
                this.view[loc.getR() * this.width + loc.getC()] = 'A';
            } else {
                this.view[loc.getR() * this.width + loc.getC()] = 'C';
                Location rloc = RackGroup.INSTANCE.getRackById(task.getRackId()).getLocation();
                this.view[rloc.getR() * this.width + rloc.getC()] = '.';
            }
        });
        this.buffer[this.p++] = String.valueOf(this.view);
        if (this.p == 128) {
            this.p = 0;
            for (String s: this.buffer) {
                try {
                    this.writer.write(s);
                    this.writer.write("\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (done) {
            try {
                for (int i = 0; i < this.p; ++i) {
                    this.writer.write(this.buffer[i]);
                    this.writer.write("\n");
                }
                this.writer.close();
                this.statsWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }   
        }
    }
}