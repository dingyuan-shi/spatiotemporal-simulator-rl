import collections

import numpy as np
from abc import ABC, abstractmethod
from typing import Any, List

import torch
from torch import Tensor


class AbsAgent(ABC):
    def __init__(self, finite: bool = True):
        self._learn = True
        self._finite = finite

    def set_learn(self):
        self._learn = True

    def set_eval(self):
        self._learn = False

    @property
    def learn(self):
        return self._learn

    @property
    def finite(self):
        return self._finite

    @abstractmethod
    def choose_action(self, single_state: np.array) -> Any:
        raise NotImplementedError

    @abstractmethod
    def update(self, states: List[np.array], actions: List[np.array], rewards: List[np.array],
               next_states: List[np.array], dones: List[bool], infos: List[Any], update_time: int = -1) -> Any:
        raise NotImplementedError

    @staticmethod
    def to_tensor(states: List[np.array], rewards: List[np.array], next_states: List[np.array], dones: List[bool],
                  device: torch.device, state_dim: int, finite: bool):
        dones = [done and finite for done in dones]
        states = Tensor(np.array(states)).view(-1, state_dim).to(device)
        dones = Tensor(np.array(dones)).view(-1, 1).to(device)
        rewards = Tensor(np.array(rewards)).view(-1, 1).to(device)
        next_states = Tensor(np.array(next_states)).view(-1, state_dim).to(device)
        return states, rewards, next_states, dones

    @abstractmethod
    def get_parameters(self) -> Any:
        raise NotImplementedError

    @abstractmethod
    def load_parameters(self, params: Any) -> None:
        raise NotImplementedError
