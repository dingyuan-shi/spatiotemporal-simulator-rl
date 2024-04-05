from .abs_env import AbsEnv
from typing import Any, Tuple
import sys
sys.path.append("envs")
from engines.taxi_python.simulator.starter import Simulator



class TaxiEnv(AbsEnv):
    
    def __init__(self):
        super().__init__()
        self.simulator = Simulator()

    def reset(self, *args, **kwargs):
        cur_day = kwargs['date']
        start_time = kwargs['start_time']
        finish_time = kwargs['finish_time']
        self.simulator.update_day_reset(cur_day, start_time, finish_time)
        observation, _, _, _ = self.step(dict(matching=None, repo=None))
        return observation
    
    def step(self, action):
        observation, reward, finish, info = self.simulator.step(action)
        return observation, reward, finish, info

    def capture(self, *args, **kwargs):
        pass
