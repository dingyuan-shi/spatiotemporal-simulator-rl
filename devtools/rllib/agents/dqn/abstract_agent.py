from abc import ABC
from typing import Tuple
import numpy as np
from rllib.agents import AbsAgent


class AbsDQNAgent(AbsAgent, ABC):

    def __init__(self, eps: Tuple[float, float], finite: bool = True):
        super().__init__(finite)
        self.eps_begin, self.eps_end = eps
        self.eps, self.eps_cnt = self.eps_begin, 0

    def update_eps(self):
        self.eps_cnt += 1
        self.eps = self.eps_end + (self.eps_begin - self.eps_end) * np.exp(-self.eps_cnt / 1000)

    @staticmethod
    def update_target_network_hard(target, source, counter, freq) -> bool:
        if counter % freq == 0:
            target.load_state_dict(source.state_dict())
            return True
        return False

    @staticmethod
    def update_target_network_soft(destination, source, tau) -> None:
        for dst_param, src_param in zip(destination.parameters(), source.parameters()):
            dst_param.data.copy_(dst_param.data * (1.0 - tau) + src_param.data * tau)
