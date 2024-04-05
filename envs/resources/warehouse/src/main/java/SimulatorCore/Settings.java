package SimulatorCore;

import java.nio.file.Paths;
import UserConfig.Configurations;

import static UserConfig.Configurations.DATA_PATH;

public class Settings {
    // 单例模式 外观模式 代理模式 原型模式 状态模式(remain) 流数据处理
    public static final int TASK_BUFFER_SIZE = 100;
    // const do not change!
    public static final String TASK_DATA_PATH = Paths.get(
            DATA_PATH, Configurations.LAYOUT_FILE_NAME + ".parquet").toString();
}
