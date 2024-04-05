# Core

Core是真正的模拟器，它有以下四个部分组成

​	1 model/：用户提交的用于测试的模型

​	2 settings.py: 模拟器参数设置

​	3 preprocessing/：预处理模块

​	4 simulator/：模拟模块  **真正的核心 前面3个粗略一看**

**运行模拟器命令**

​	python -m Core.simulator.starter <参数选项> <参数值>

​	<参数选项>: -s: 确定模拟开始时间，以秒为单位，默认为 "4 * 3600"

​						-f: 确定结束开始时间，以秒为单位，默认为 "24 * 3600"

​						-b: 模拟的开始天数 整数 默认为6

​						-e：模拟结束的天数默认为6，（模拟会包含开始和结束的两天，例如设置-b 6 -e 7，那么会模拟第6、7两天）

​						-r: 调度算法执行频率，以秒为单位，默认为"5 * 60"

​						-l: log的频率，以秒为单位，默认为"20 * 60"

​						-d: 随机种子，整数，默认为0

例如 python3 -m Core.simulator.starter -s "4 * 3600" -f "7 * 3600" -b 6 -e 6 -r 300 -l 3600 -d 0

**注意**：如果参数的值中间有空格或其他字符，需要用引号引起来，否则参数解析会报错，例如"4 * 3600"

上述事例指令含义为：模拟第6天从4点～7点，调度算法每300s执行一次，每隔一小时把统计数据写入log，随机种子为0。

统计结果见log文件，路径为settings.py中设置的DATA_PATH，文件名为"stats_rec201611%02d.csv"



## 1 model

用户提交的用于测试的模型，模拟器会调用其中的两个接口，为model/agent.py中的**Agent.dispatch(self, dispatch_observ)**和**Agent.reposition(repo_observ)**，分别执行分单算法和空车调度算法。具体参数和返回值详见model/agent.py中的注释。

对于交互界面，用户应该提交一个自己模型的压缩包，后端收到后解压放在model/路径下即可。



## 2 settings.py

模拟器参数设置，设置了一些常量和变量，比如数据存放的路径、模拟中的有关设置等等，具体见settings.py

## 3 preprocessing

预处理模块有4个文件，这些文件会处理指定路径下的数据，路径在settings.py中设置。并把有关的处理结果放在上述路径下。**预处理模块只要不更换原始数据就不需要执行**。它的作用主要是把一些csv文件中的内容变成python支持的格式，方便模拟器直接通过pickle load加载。4个文件分别为：

**gen_driver_distribution.py**

​	司机随着时间变化的上下线的数量。	

**gen_driver_trans_prob.py**

​	空车的转移概率，数据集中的csv文件，记录了不同六边形网格中司机会转移到临近六边形的概率，这个文件的作用是把它变成python字典，方便加载

**gen_geo.py**

​	地理信息，主要是生成simulator/grid.py所需要的索引。从而实现快速查找坐标所在的六边形网格，快速查找每个网格周围的网格等等操作。

**gps_to_track.py**

​	把gps的csv文件转化为接单后的轨迹，为可视化部分提供支持

运行这些文件的方法为 python3 -m Core.preprocessing.<文件名> 注意文件名不包含拓展名.py，例如

python3 -m Core.preprocessing.gen_driver_distribution

## 4 Simulator

真正的核心部分。

有以下6个文件组成

**dataloader.py**

​	加载数据，从settings.py中指定的路径加载原始数据或预处理模块处理后的数据，主要加载了订单和司机。

​	load_data和skip_to用于加载指定日期的数据并跳转到指定的模拟开始时刻。

​	get_orders和get_drivers用来获取模拟到某一时刻的订单和司机。

**entity.py**

​	定义了模拟中用到的对象

​	其中最为重要的是司机，规定了司机的有关动作，具体方法的功能已经在代码中注释

​	其他的Order（订单）、DGPair（司机和所在的网格，用于调度）、RepoInfo（调度信息）、DispatchElem（分单信息）简化为namedtuple

**driver_group.py**

​	entity中的Driver只定义了单个司机的行为，driver_group实现对司机的集体管理。具体实现了分单、调度、更新等操作，最为重要的维护了三个字典avail_driver_dict、grid_to_drivers和serve_driver_dict，分别记录了可以接单的司机（REPO和IDLE状态）、可以网格中包含的可以接单的司机、正在服务的司机。该模块不断更新司机的状态，调整这3个字典的内容。

**utils.py**

​	convert_to_timestamp 把一天的秒数转化为时间戳

​	acc_dist 根据坐标计算准确的地球表面最短距离

​	sec_to_human：把一天的描述转化为XX:XX的形式方便人类阅读

**grids.py**

​	这个模块主要支持了一些空间查询操作

​	具体的方法的功能详见注释

**starter.py**

​	真正的模拟过程

​	最为核心的start_simulation函数分5个步骤，详见注释

