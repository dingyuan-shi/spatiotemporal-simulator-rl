package UserConfig;

import SimulatorCore.*;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import Solvers.GMAPF.Solver;

import com.opencsv.exceptions.CsvValidationException;
import com.alibaba.fastjson.JSON;

public class Configurations {
    
    public static boolean IGNORE_PICKING_TIME = true;
    public static final String ROOT_PATH = System.getenv("DSS_DIR") + "/workdir/data/warehouseData";
    public static final String DATA_PATH = Paths.get(ROOT_PATH, "transactions").toString();
    public static final String LAYOUT_PATH = Paths.get(ROOT_PATH, "layouts").toString();
    public static final String REC_PATH = System.getenv("DSS_DIR") + "/workdir/result/testWarehouse";
    public static String EVAL_PATH;

    public static Service service;
    public static String LAYOUT_FILE_NAME;
    public static int LOG_SEG;
    public static String LAYOUT_FILE_PATH;
    public static String TASK_FILE_PATH;
    public static String RACK_TO_PICKERS_PATH;
    public static PrintStream ps;

    public static void setConfigurations(String[] args) {
        String method = args[0];
        String layout = args[1];
        String evalName = "";
        // 创建eval文件记录路径
        EVAL_PATH = Paths.get(REC_PATH, evalName).toString();
        // System.out.println(EVAL_PATH);
        File evalDir = new File(EVAL_PATH);
        evalDir.mkdirs();
        
        if (layout.indexOf('_') == -1) {
            LAYOUT_FILE_NAME = layout;
        } else {
            LAYOUT_FILE_NAME = layout.substring(0, layout.indexOf('_'));
        }
        
        TASK_FILE_PATH = Paths.get(DATA_PATH, "tasks_" + layout + ".csv").toString();
        RACK_TO_PICKERS_PATH = Paths.get(DATA_PATH, "rackToPickers_" + layout + ".csv").toString();
        switch (LAYOUT_FILE_NAME) {
            case "synA": 
                LOG_SEG = 100; 
                break;
            case "synB": 
                LOG_SEG = 100; 
                break;
            case "real1": 
                LOG_SEG = 1000; 
                break;
            case "real2": 
                LOG_SEG = 1000; 
                break;
            case "real3":
                LOG_SEG = 1000;
            default:
                LOG_SEG = 10;
        }
        LAYOUT_FILE_PATH = Paths.get(LAYOUT_PATH, LAYOUT_FILE_NAME + ".txt").toString();
        switch (method) {
            case "": service = new Solvers.DummyServer(); break;
            case "SAP": service = new Solvers.SAP.Solver(); break;
            case "TWP": service = new Solvers.TWP.Solver(); break;
            case "ACP": service = new Solvers.ACP.Solver(); break;
            case "RP": service = new Solvers.RP.Solver(); break;
            case "GMAP": service = new Solvers.GMAPF.Solver(LAYOUT_FILE_PATH); break;
            
            default:
                service = null;
                System.out.println("No specified algorithm name");
                System.exit(1);
        }
        try {
            ps = System.out;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws CsvValidationException, NumberFormatException {
        // Configurations.setConfigurations(args);
        // System.setOut(Configurations.ps);
        // TaskPool.INSTANCE.setIgnorePickingTime(Configurations.IGNORE_PICKING_TIME);
        // // init user defined Service
        // Configurations.service.initialize(PickerGroup.INSTANCE, RackGroup.INSTANCE);
        // System.out.println("testing " + Configurations.service.getClass().getName());
        // // simulate
        // ServerProxy sp = new ServerProxy(Configurations.service);
        // HashMap<String, Object> res = ClockDriver.simulate(sp);
        // // out put statistics
        // System.out.println("total real time usage: " + (Long)res.get("totalUsage") / 1000.0 + "s");
        // System.out.println("selection time usage: " + (Long)res.get("matchingUsage") / 1000.0 + "s");
        // System.out.println("planning time usage: " + (Long)res.get("planningUsage") / 1000.0 + "s");
        // System.out.println("Total time usage: " + res.get("simulationStep"));
        // System.out.println("Memory Usage: " + Arrays.toString((double[]) res.get("memory")));
        // Configurations.ps.flush();
        // Configurations.ps.close();
        // String[] targs = new String[2];
        // targs[0] = "SAP";
        // targs[1] = "small";
        Engine.reset(args);
        // Engine.resetJSON("small");
        Pair<Pair<HashMap<Integer, Location>, HashMap<Integer, Task>>, Pair<ArrayList<Robot>,  ArrayList<Robot>>> observ;
        boolean done = false;
        // Engine.stepJSON("{}",  "{4484:389}", "{389:{\"s\":3,\"t\":[{0:{\"c\":198,\"r\":98}},{1:{\"c\":198,\"r\":97}},{17:{\"c\":214,\"r\":97}},{18:{\"c\":214,\"r\":98}},{19:{\"c\":214,\"r\":97}},{20:{\"c\":214,\"r\":96}},{21:{\"c\":214,\"r\":95}},{24:{\"c\":214,\"r\":92}},{25:{\"c\":214,\"r\":91}},{26:{\"c\":214,\"r\":90}},{27:{\"c\":214,\"r\":89}},{30:{\"c\":214,\"r\":86}},{31:{\"c\":214,\"r\":85}},{32:{\"c\":214,\"r\":84}},{33:{\"c\":214,\"r\":83}},{36:{\"c\":214,\"r\":80}},{37:{\"c\":214,\"r\":79}},{38:{\"c\":214,\"r\":78}},{39:{\"c\":214,\"r\":77}},{42:{\"c\":214,\"r\":74}},{43:{\"c\":214,\"r\":73}},{44:{\"c\":214,\"r\":72}},{258:{\"c\":0,\"r\":72}},{259:{\"c\":0,\"r\":71}},{260:{\"c\":0,\"r\":70}}]}}");
        ServerProxy sp = new ServerProxy(Configurations.service);
        int t = 0;
        while (!done) {
            observ = Engine.getObserv();
            // execute the algorithm
            HashMap<Integer, Integer> assignment = sp.assign(t, observ.getRight().getRight(), observ.getLeft().getRight());
            HashMap<Integer, Path> returnRobotIdToPath = sp.planning(t,  observ.getRight().getLeft());
            HashMap<Integer, Path> robotIdToPath = sp.planning(t, assignment, observ.getLeft().getRight());
            done = Engine.step(returnRobotIdToPath, assignment, robotIdToPath);
            ++t;
        }
    }
}
