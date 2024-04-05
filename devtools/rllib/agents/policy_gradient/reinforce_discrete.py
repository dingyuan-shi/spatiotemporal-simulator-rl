import torch
import numpy as np
from typing import Any
from .abstract_agents import ReinforceAgent
from rllib.models import AbsDiscretePolicy


class ReinforceDiscreteAgent(ReinforceAgent):

    def __init__(self, gamma: float, observ_dim: int, n_action: int, lr: float, device: torch.device,
                 model: AbsDiscretePolicy, finite: bool = True):
        super().__init__(gamma, observ_dim, n_action, lr, device, model, finite)

    def choose_action(self, single_observ: np.array) -> Any:
        single_observ = torch.Tensor(single_observ).to(self.device)
        with torch.no_grad():
            action = torch.distributions.Categorical(self.net(single_observ)).sample().squeeze().item()
            if self.learn:
                return action, None
            else:
                return action
