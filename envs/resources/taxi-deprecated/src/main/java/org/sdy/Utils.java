package org.sdy;

import java.util.Calendar;

public class Utils {
    public static int secondToTimestamp(int second, int year, int month, int day) {
        int hour = second / 3600;
        int minute = (second - hour * 3600) / 60;
        int sec = second % 60;
        
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, day, hour, minute, sec);
        return (int)(cal.getTime().getTime() / 1000);
    }

    public static String secondToHuman(int second) {
        int hour = second / 3600;
        int minute = (second - hour * 3600) / 60;
        int sec = second % 60;
        return String.format("%02d:%02d:%02d", hour, minute, sec);
    }
}
