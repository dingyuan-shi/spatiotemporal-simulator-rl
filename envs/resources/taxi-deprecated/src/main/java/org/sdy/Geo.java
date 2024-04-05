package org.sdy;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Geo {

    public static class GeoData {
        private final Map<String, String[]> gridAdjacent;
        private final Map<String, double[]> gridCenters;
        private final String[] gridIds;
        private final HashMap<String, Integer> gridIdsToNum;
        private final Map<String, double[][]> gridInfos;
        private final ArrayList<ArrayList<String[]>> mesh;
        private final double stepLng, stepLat, minLng, minLat;
        private final double[] cityCenter;
        public GeoData(Map<String, String[]> gridAdjacent, Map<String, double[]> gridCenters, String[] gridIds,
                       Map<String, double[][]> gridInfos, ArrayList<ArrayList<String[]>> mesh,
                       double stepLng, double stepLat, double minLng, double minLat, double[] cityCenter) {
            this.gridAdjacent = gridAdjacent;
            this.gridCenters = gridCenters;
            this.gridIds = gridIds;
            this.gridInfos = gridInfos;
            this.mesh = mesh;
            this.stepLng = stepLng;
            this.stepLat = stepLat;
            this.minLat = minLat;
            this.minLng = minLng;
            this.cityCenter = cityCenter;
            this.gridIdsToNum = new HashMap<>();
            int tmpNo = 0;
            for (String gridId: gridIds) {
                gridIdsToNum.put(gridId, tmpNo++);
            }
        }
    }

    private static final int[] dx = {0, 0, 0, 1, 1, 1, -1, -1, -1};
    private static final int[] dy = {0, 1, -1, 1, 0, -1, 0, 1, -1};
    private static final GeoData data = loadData();

    private static GeoData loadData() {
        try {
            return JSON.parseObject(Files.newBufferedReader(
                    Paths.get(GlobalSetting.dataRoot.toString(), "geo_info.json")
            ).readLine(), GeoData.class);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Please check the geoInfo file");
            System.exit(1);
        }
        return null;
    }

    public static double distance(double lng1, double lat1, double lng2, double lat2) {
        return Math.hypot((lng1 - lng2), (lat1 - lat2)) * 110000;
    }

    public static double distance(Pair<Double, Double> lngLat1, Pair<Double, Double> lngLat2) {
        return Geo.distance(lngLat1.getLeft(), lngLat1.getRight(), lngLat2.getLeft(), lngLat2.getRight());
    }

    public static double distance(Pair<Double, Double> lngLat1, Pair<Double, Double> lngLat2, char accurateFlag) {
        return Geo.distance(lngLat1.getLeft(), lngLat1.getRight(), lngLat2.getLeft(), lngLat2.getRight(), accurateFlag);
    }

    public static double distance(double lng1, double lat1, double lng2, double lat2, char accurateFlag) {
        double delta_lat = (lat1 - lat2) / 2.;
        double delta_lng = (lng1 - lng2) / 2.;
        double arc_pi = 3.14159265359 / 180.;
        return 2 * 6378137 * Math.asin(
                Math.sqrt(
                        Math.pow(Math.sin(arc_pi * delta_lat), 2) +
                                Math.cos(arc_pi * lat1) * Math.cos(arc_pi * lat2) *
                                        Math.pow(Math.sin(arc_pi * delta_lng), 2)
                )
        );
    }

    public static int getGridNum(String gridId) {
        return data.gridIdsToNum.get(gridId);
    }

    public static int getTotalGridNum() {
        return data.gridIdsToNum.size();
    }

    public static String[] findGridMore(String gridId) {
        return data.gridAdjacent.get(gridId);
    }

    public static String findGrid(double lng, double lat) {
        int i = (int)((lng - data.minLng) / data.stepLng) + 1;
        int j = (int)((lat - data.minLat) / data.stepLat) + 1;
        double minDistance = 90000000;
        String recGridId = data.gridIds[0];
        for (int k = 0; k < 9; k++) {
            try {
                Map<String, Double> gridIdDis = findClosest(lng, lat, data.mesh.get(i + dx[k]).get(j + dy[k]));
                String tmpGrid = gridIdDis.keySet().iterator().next();
                double tmpDis = gridIdDis.get(tmpGrid);
                if (tmpDis < minDistance) {
                    minDistance = tmpDis;
                    recGridId = tmpGrid;
                }
            } catch (NullPointerException e) { }
            catch (IndexOutOfBoundsException e) { }
        }
        return recGridId;
    }

    public static String findGrid(double lng, double lat, String gridId) {
        double[] lngLat = data.gridCenters.get(gridId);
        if (distance(lng, lat, lngLat[0], lngLat[1]) < 250) {
            return gridId;
        }
        Map<String, Double> gridIdDis = findClosest(lng, lat, data.gridAdjacent.get(gridId));
        return gridIdDis.keySet().iterator().next();
    }

    private static Map<String, Double> findClosest(double lng, double lat, String[] grids)  {
        double minDistance = 90000000;
        String recGridId = "";
        HashMap<String, Double> res = new HashMap<>();
        for (String grid: grids) {
            double[] tmpLngLat = data.gridCenters.get(grid);
            double dis = distance(lng, lat, tmpLngLat[0], tmpLngLat[1]);
            if (dis < 250) {
                res.put(grid, dis);
                return res;
            }
            if (minDistance > dis) {
                minDistance = dis;
                recGridId = grid;
            }
        }
        res.put(recGridId, minDistance);
        return res;
    }

    public static double awayFromCenter(String gridId) {
        double[] lngLat = data.gridCenters.get(gridId);
        return distance(lngLat[0], lngLat[1], data.cityCenter[0], data.cityCenter[1], 'a');
    }

    public static double[] getGridLocation(String gridId) {
        return data.gridCenters.get(gridId);
    }

    public static double[] genRandom(String gridId) {
        double[] location = new double[2];
        double[][] points = data.gridInfos.get(gridId);
        double[] weights = new double[6];
        double denom = 0;
        for (int i = 0; i < 6; i++) {
            weights[i] = Math.random();
            denom += weights[i];
        }
        for (int i = 0; i < 6; i++) {
            weights[i] /= denom;
            location[0] += weights[i] * points[i][0];
            location[1] += weights[i] * points[i][1];
        }
        return location;
    }

    public static String[] getGridIds() {
        return data.gridIds;
    }
}
