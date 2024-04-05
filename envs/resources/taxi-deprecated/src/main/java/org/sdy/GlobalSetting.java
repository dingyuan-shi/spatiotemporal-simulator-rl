package org.sdy;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

public class GlobalSetting {
    public static final int SECONDS_PER_MINUTE = 60;
    public static final int SECONDS_PER_HOUR = 3600;
    public static final int UPDATE_DRIVER_LOG_ON = 5 * SECONDS_PER_MINUTE; // 司机上下线更新的频率 目前不能改
    public static int UPDATE_DRIVER_LOG_OFF = 5 * SECONDS_PER_MINUTE;
    public static int TRANS_PATTERN_FREQUENCY = SECONDS_PER_HOUR;
    public static final int IDLE_TRANSITION_FREQUENCY = SECONDS_PER_MINUTE;  // 空车位置随机变化的频率 这个用户不能改
    public static int IDLE_UPDATE_FREQUENCY = IDLE_TRANSITION_FREQUENCY / 2;
    public static final int REPO_UPDATE_FREQUENCY = 15;  // 被调度的司机多长时间更新一次位置 这个用户不能改
    public static double SPEED = 3.;
    public static Path dataRoot = null;
    public static Path orderRoot = null;
    public static double DRIVER_RATIO = 3.;
    public static int REPO_FREQUENCY = 5 * SECONDS_PER_MINUTE;
    public static int startTime = 4 * SECONDS_PER_HOUR;
    public static int endTime = 10 * SECONDS_PER_HOUR;
    public static String startDay = "20161106";
    public static String endDay = "20161106";
    public static int LOG_FREQUENCY = 10 * SECONDS_PER_MINUTE;
    public static int CORE_NUM = 2;
    public static boolean IS_UPDATE_CONCURRENT = true;
    public static boolean IS_GET_AVAIL_CONCURRENT = true;
    public static boolean IS_REPO_CAN_SERVE = true;  // 被调度车辆在调度过程中能否接单
    public static final int UPDATE_CONCURRENT_THRESHOLD = 10000;
    public static final int GET_AVAIL_CONCURRENT_THRESHOLD = 10000;
    public static int seed = 0;
    public static String user = "default";


    static {
        Properties props = new Properties();
		try {
			props.load(GlobalSetting.class.getResourceAsStream("config.properties"));
			SPEED = Double.parseDouble(props.getProperty("SPEED"));	
            dataRoot = Paths.get((String)(props.getProperty("dataRoot")));
            orderRoot = Paths.get(dataRoot.toString(), "total_ride_request");
            DRIVER_RATIO = Double.parseDouble(props.getProperty("DRIVER_RATIO"));	
            REPO_FREQUENCY = Integer.parseInt(props.getProperty("REPO_FREQUENCY"));	
            startTime = Integer.parseInt(props.getProperty("startTime"));
            endTime = Integer.parseInt(props.getProperty("endTime"));
            startDay = (String)(props.getProperty("startDay"));
            endDay = (String)(props.getProperty("endDay"));
            LOG_FREQUENCY = Integer.parseInt(props.getProperty("LOG_FREQUENCY"));
            CORE_NUM = Integer.parseInt(props.getProperty("CORE_NUM"));
            IS_UPDATE_CONCURRENT = Boolean.parseBoolean(props.getProperty("IS_UPDATE_CONCURRENT"));
            IS_GET_AVAIL_CONCURRENT = Boolean.parseBoolean(props.getProperty("IS_GET_AVAIL_CONCURRENT"));
            IS_REPO_CAN_SERVE = Boolean.parseBoolean(props.getProperty("IS_REPO_CAN_SERVE"));
            seed = Integer.parseInt(props.getProperty("seed"));
            user = (String)(props.getProperty("user"));
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
}
