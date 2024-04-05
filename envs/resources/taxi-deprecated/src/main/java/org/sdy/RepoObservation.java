package org.sdy;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;

public class RepoObservation {
    private int timestamp;
    private int dayOfWeek;

    private ArrayList<Pair<String, Integer>> gridIdDriverId;

    public RepoObservation(int timestamp, int dayOfWeek, ArrayList<Pair<String, Integer>> gridIdDriverId) {
        this.timestamp = timestamp;
        this.dayOfWeek = dayOfWeek;
        this.gridIdDriverId = gridIdDriverId;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public ArrayList<Pair<String, Integer>> getGridIdDriverId() {
        return gridIdDriverId;
    }

}

