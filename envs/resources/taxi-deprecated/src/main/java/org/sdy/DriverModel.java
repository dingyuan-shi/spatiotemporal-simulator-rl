package org.sdy;

import com.alibaba.fastjson.JSON;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

public class DriverModel {
    public static class DriverModelData {
        double[] logOff;
        double[] logOn;
        HashMap<String, Double[]>[] transProb;
        HashMap<String, String[]>[] transGrid;
        public DriverModelData(double[] logOff, double[] logOn,
                               HashMap<String, Double[]>[] transProb,
                               HashMap<String, String[]>[] transGrid) {
            this.logOff = logOff;
            this.logOn = logOn;
            this.transProb = transProb;
            this.transGrid = transGrid;
        }
    }

    private static final HashMap<String, Double> gridCoefficient = loadGridCoefficient();
    private static final DriverModelData driverModelData = loadDriverModelData();

    static HashMap<String, Double> loadGridCoefficient() {
        HashMap<String, Double> gridCoefficient = new HashMap<String, Double>();
        double normDenominator = 0;
        for (String gridId: Geo.getGridIds()) {
            double coeff = Math.max(0, 0.4 - (int)(Geo.awayFromCenter(gridId) / 2000) * 0.1);
            normDenominator += coeff;
            gridCoefficient.put(gridId, coeff);
        }
        for (String gridId: Geo.getGridIds()) {
            gridCoefficient.put(gridId, gridCoefficient.get(gridId) / normDenominator);
        }
        return gridCoefficient;
    }

    static DriverModelData loadDriverModelData() {
        try {
            return JSON.parseObject(Files.newBufferedReader(
                    Paths.get(GlobalSetting.dataRoot.toString(), "driver_model.json")
            ).readLine(), DriverModelData.class);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Please check the driver_model file");
            System.exit(1);
        };
        return null;
    }

    public static ArrayList<Driver> logOn(int currentSecond) {
        int currentSegment = currentSecond / GlobalSetting.UPDATE_DRIVER_LOG_ON;
        ArrayList<Driver> newDrivers = new ArrayList<>();
        double totalTargetNumber = driverModelData.logOn[currentSegment] * GlobalSetting.DRIVER_RATIO;
        for (String gridId: Geo.getGridIds()) {
            for (int i = 0; i < (int)(totalTargetNumber * gridCoefficient.get(gridId)); i++) {
                newDrivers.add(new Driver(gridId, currentSecond));
            }
        }
        return newDrivers;
    }

    public static boolean logOff(int currentSecond, Driver driver) {
        int currentSegment = currentSecond / GlobalSetting.UPDATE_DRIVER_LOG_OFF;
        return Math.random() < driverModelData.logOff[currentSegment];
    }

    public static String transToNext(int currentSecond, String currentGridId) {
        int currentSegment = currentSecond / GlobalSetting.TRANS_PATTERN_FREQUENCY;
        Double[] prob = driverModelData.transProb[currentSegment].get(currentGridId);
        if (prob == null) return currentGridId;
        double r = Math.random();
        for (int i = 0; i < prob.length; i++) {
            r -= prob[i];
            if (r < 0.0001) {
                return driverModelData.transGrid[currentSegment].get(currentGridId)[i];
            }
        }
        return currentGridId;
    }

    public static void main(String[] args) {
        DriverModel.logOn(0);
    }
}
