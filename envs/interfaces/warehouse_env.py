from typing import Any, Tuple
from .abs_env import AbsJarEnv
import json
from os.path import join, abspath, dirname


CLASS_PATH = join(dirname(abspath(__file__)), "../engines/", "warehouse-1.0-SNAPSHOT.jar")
CLASS_NAME = "SimulatorCore.Engine"

class WarehouseEnv(AbsJarEnv):

    def __init__(self) -> None:
        super().__init__(CLASS_NAME, CLASS_PATH)

    def reset(self, layout: str) -> Any:
        pickerInfo, availTasks, readyReturnRobot, availRobot = self.rawEnv.resetJSON(layout)
        pickerInfo = eval(pickerInfo)
        availTasks = eval(availTasks)
        readyReturnRobot = json.loads(readyReturnRobot)
        availRobot = json.loads(availRobot)
        return (pickerInfo, availTasks, readyReturnRobot, availRobot) 
    
    def step(self, action: Any) -> Tuple[Any, Any, bool, Any]:
        returnRobotIdToPath, assignment, robotIdToPath = action
        if not isinstance(returnRobotIdToPath, str):
            returnRobotIdToPath = json.dumps(returnRobotIdToPath)
        if not isinstance(assignment, str):
            assignment = json.dumps(assignment)
        if not isinstance(robotIdToPath, str):
            robotIdToPath = json.dumps(robotIdToPath)
        pickerInfo, availTasks, readyReturnRobot, availRobot = self.rawEnv.stepJSON(returnRobotIdToPath, assignment, robotIdToPath)
        if pickerInfo == "":
            return None, -1, True, None
        pickerInfo = eval(pickerInfo)
        availTasks = eval(availTasks)
        readyReturnRobot = json.loads(readyReturnRobot)
        availRobot = json.loads(availRobot)
        return (pickerInfo, availTasks, readyReturnRobot, availRobot), -1, False, None

    def capture(self, *args, **kwargs):
        pass
