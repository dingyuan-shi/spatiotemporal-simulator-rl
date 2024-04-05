from abc import ABC
from typing import Tuple, Any
import torch
import torch.nn as nn


class AbsDiscretePolicy(nn.Module, ABC):
    def __init__(self, state_dim: int, n_action: int):
        super().__init__()
        self.state_dim = state_dim
        self.n_action = n_action

    @staticmethod
    def forward(self, states: torch.Tensor) -> torch.Tensor:
        """
        requires a distribution of |A| length
        :param self:
        :param states:
        :return:
        """
        raise NotImplementedError

    @staticmethod
    def log_probs(self, batch_states: torch.Tensor, batch_actions: torch.Tensor) -> torch.Tensor:
        raise NotImplementedError


class AbsContinuousPolicy(nn.Module, ABC):
    def __init__(self, state_dim: int, action_dim: int, action_range: Tuple[Any, Any]) -> None:
        super().__init__()
        self.state_dim = state_dim
        self.action_dim = action_dim
        self.action_low = torch.Tensor(action_range[0]).view(action_dim)
        self.action_high = torch.Tensor(action_range[1]).view(action_dim)
        self.action_mid = (self.action_low + self.action_high) / 2
        self.action_width = self.action_high - self.action_mid

    def forward(self, states: torch.Tensor) -> Tuple[torch.Tensor, torch.Tensor]:
        """
        :param states:
        :return: two tensors, corresponding mu and sigma
        """
        raise NotImplementedError

    def log_probs(self, batch_states: torch.Tensor, batch_actions: torch.Tensor) -> torch.Tensor:
        raise NotImplementedError
