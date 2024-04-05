from typing import Any, Tuple
from .abs_env import AbsJarEnv
import json
from os.path import join, abspath, dirname


CLASS_PATH = join(dirname(abspath(__file__)), "../engines/", "taxi-1.0-SNAPSHOT.jar")
CLASS_NAME = "org.sdy.Engine"

class TaxiEnv(AbsJarEnv):

    def __init__(self) -> None:
        super().__init__(CLASS_NAME, CLASS_PATH)

    def initialize_env(self, *args, **kwargs) -> None:
        date = 20161106
        config_path = ""
        if len(args) > 0:
            date = int(args[1])
        if "date" in kwargs:
            date = kwargs["date"]
        if "config_path" in kwargs:
            config_path = kwargs["config_path"]
        self.env = self.rawEnv(date, config_path)

    def reset(self, *args, **kwargs) -> Any:
        self.initialize_env(*args, **kwargs)
        res = self.step((None, None))
        return res[0]
    
    def step(self, action: Any) -> Tuple[Any, Any, bool, Any]:
        matching_action, repo_action = action
        matching_action = json.dumps(matching_action)
        repo_action = json.dumps(repo_action)
        mo, ro, reward = self.env.stepJSON(matching_action, repo_action)
        if mo == "":
            return None, reward, True, None
        mo = json.loads(mo)
        ro = json.loads(ro)
        return (mo, ro), reward, False, None

    def capture(self, *args, **kwargs):
        pass
