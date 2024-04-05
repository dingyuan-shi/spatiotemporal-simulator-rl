package testEngine;

import java.util.ArrayList;
import java.util.HashMap;

import com.opencsv.exceptions.CsvValidationException;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import SimulatorCore.Engine;
import SimulatorCore.Location;
import SimulatorCore.Path;
import SimulatorCore.Robot;
import SimulatorCore.ServerProxy;
import SimulatorCore.Task;


public class testEngine {
    @Test
    public void testEngine() throws CsvValidationException, NumberFormatException {
        Engine.reset("");
        Pair<Pair<HashMap<Integer, Location>, HashMap<Integer, Task>>, Pair<ArrayList<Robot>,  ArrayList<Robot>>> observ;
        boolean done = false;
        ServerProxy sp = new ServerProxy(new Solvers.SAP.Solver());
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
