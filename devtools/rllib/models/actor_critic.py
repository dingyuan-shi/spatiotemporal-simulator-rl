from abc import ABC
from typing import Tuple

from torch import nn, Tensor


class AbsVCritic(nn.Module, ABC):
    def __init__(self, state_dim: int) -> None:
        super().__init__()
        self.state_dim = state_dim

    @staticmethod
    def forward(self, states: Tensor) -> Tensor:
        """
        requires output a tensor with shape (-1, 1)
        :param self:
        :param states:
        :return:
        """
        raise NotImplementedError


class AbsDeterministicContinuousActor(nn.Module, ABC):

    def __init__(self, state_dim: int, action_dim: int, action_range) -> None:
        super().__init__()
        self.state_dim = state_dim
        self.action_dim = action_dim
        self.action_low = Tensor(action_range[0]).view(action_dim)
        self.action_high = Tensor(action_range[1]).view(action_dim)
        self.action_mid = (self.action_low + self.action_high) / 2
        self.action_width = self.action_high - self.action_mid

    def forward(self, states: Tensor) -> Tensor:
        """
        requires a actor with action dim
        :param states: 
        :return: 
        """
        raise NotImplementedError


class AbsNafQ(nn.Module, ABC):

    def __init__(self, state_dim: int, action_dim: int, action_range: Tuple) -> None:
        super().__init__()
        self.action_dim = action_dim
        self.state_dim = state_dim
        self.action_low = Tensor(action_range[0]).view(action_dim)
        self.action_high = Tensor(action_range[1]).view(action_dim)
        self.action_mid = (self.action_low + self.action_high) / 2
        self.action_width = self.action_high - self.action_mid

    def forward(self, state: Tensor) -> Tuple[Tensor, Tensor, Tensor]:
        """
        requires outputing three tensor mu, l and v
        :param state:
        :return:
        """
        raise NotImplementedError
